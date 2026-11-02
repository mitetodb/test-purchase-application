package app.repository;

import app.TestPurchaseApplication;
import app.security.LoginRateLimitKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
class LoginRateLimitUpsertMySqlIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("test_purchase_db")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private LoginRateLimitUpsert loginRateLimitUpsert;

    @Autowired
    private LoginRateLimitRepository loginRateLimitRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clearBuckets() {
        transactionTemplate.executeWithoutResult(status -> loginRateLimitRepository.deleteAll());
    }

    @Test
    void insertsNewKeyWithAttemptCountOne() {
        String rateKey = LoginRateLimitKeys.storageKey("ip:", "203.0.113.1");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowNotBefore = now.minusSeconds(3600);

        loginRateLimitUpsert.upsertIncrement(UUID.randomUUID(), rateKey, now, windowNotBefore);

        var row = loginRateLimitRepository.findByRateKey(rateKey).orElseThrow();
        assertEquals(1, row.getAttemptCount());
    }

    @Test
    void incrementsExistingKeyInActiveWindow() {
        String rateKey = LoginRateLimitKeys.storageKey("user:", "mysql-upsert-user");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowNotBefore = now.minusSeconds(3600);

        loginRateLimitUpsert.upsertIncrement(UUID.randomUUID(), rateKey, now, windowNotBefore);
        loginRateLimitUpsert.upsertIncrement(UUID.randomUUID(), rateKey, now, windowNotBefore);

        assertEquals(2, loginRateLimitRepository.findByRateKey(rateKey).orElseThrow().getAttemptCount());
    }

    @Test
    void resetsAttemptCountWhenWindowExpired() {
        String rateKey = LoginRateLimitKeys.storageKey("ip:", "198.51.100.20");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiredStart = now.minusHours(3);

        jdbcTemplate.update(
                """
                        INSERT INTO login_rate_limit (id, rate_key, attempt_count, window_start)
                        VALUES (?, ?, ?, ?)
                        """,
                UuidBinaryBytes.toBytes(UUID.randomUUID()),
                rateKey,
                9,
                java.sql.Timestamp.valueOf(expiredStart));

        loginRateLimitUpsert.upsertIncrement(
                UUID.randomUUID(), rateKey, now, now.minusSeconds(3600));

        assertEquals(1, loginRateLimitRepository.findByRateKey(rateKey).orElseThrow().getAttemptCount());
    }

    @Test
    void parallelUpsertsOnSameKeyReachExpectedCount() throws Exception {
        String rateKey = LoginRateLimitKeys.storageKey("ip:", "203.0.113.44");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowNotBefore = now.minusSeconds(3600);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 2; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        loginRateLimitUpsert.upsertIncrement(
                                UUID.randomUUID(), rateKey, now, windowNotBefore);
                    } catch (Throwable t) {
                        failure.compareAndSet(null, t);
                    }
                });
            }
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            pool.shutdown();
            assert pool.awaitTermination(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertNull(failure.get(), () -> "Unexpected failure: " + failure.get());
        assertEquals(2, loginRateLimitRepository.findByRateKey(rateKey).orElseThrow().getAttemptCount());
    }
}
