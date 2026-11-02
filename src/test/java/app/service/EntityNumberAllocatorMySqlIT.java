package app.service;

import app.TestPurchaseApplication;
import app.model.enums.EntityNumberSequenceName;
import app.repository.EntityNumberSequenceRepository;
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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
class EntityNumberAllocatorMySqlIT {

    private static final int PARALLEL_THREADS = 50;

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
    private EntityNumberAllocator entityNumberAllocator;

    @Autowired
    private EntityNumberSequenceRepository entityNumberSequenceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void resetShopSequence() {
        transactionTemplate.executeWithoutResult(status ->
                entityNumberSequenceRepository.findById(EntityNumberSequenceName.SHOP.name())
                        .ifPresent(row -> {
                            row.setLastValue(0L);
                            entityNumberSequenceRepository.save(row);
                        }));
    }

    @Test
    void flywayV8SeedsSequenceRowsFromEmptyTables() {
        Long customerLast = jdbcTemplate.queryForObject(
                "SELECT last_value FROM entity_number_sequence WHERE sequence_name = 'CUSTOMER'",
                Long.class);
        Long shopLast = jdbcTemplate.queryForObject(
                "SELECT last_value FROM entity_number_sequence WHERE sequence_name = 'SHOP'",
                Long.class);
        Long tpLast = jdbcTemplate.queryForObject(
                "SELECT last_value FROM entity_number_sequence WHERE sequence_name = 'TEST_PURCHASE'",
                Long.class);

        assertEquals(0L, customerLast);
        assertEquals(0L, shopLast);
        assertEquals(1000L, tpLast);
    }

    @Test
    void parallelShopNumbersOnMySqlAreUnique() throws Exception {
        Set<String> numbers = ConcurrentHashMap.newKeySet();
        CountDownLatch ready = new CountDownLatch(PARALLEL_THREADS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_THREADS);
        try {
            for (int i = 0; i < PARALLEL_THREADS; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        numbers.add(entityNumberAllocator.nextShopNumber());
                    } catch (Throwable t) {
                        failure.compareAndSet(null, t);
                    }
                });
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            pool.shutdown();
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }

        assertNull(failure.get(), () -> "Unexpected failure: " + failure.get());
        assertEquals(PARALLEL_THREADS, numbers.size());
        assertEquals(
                PARALLEL_THREADS,
                jdbcTemplate.queryForObject(
                        "SELECT last_value FROM entity_number_sequence WHERE sequence_name = 'SHOP'",
                        Long.class));
    }
}
