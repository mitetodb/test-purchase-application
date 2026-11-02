package app.security;

import app.TestPurchaseApplication;
import app.repository.LoginRateLimitRepository;
import app.repository.LoginRateLimitUpsert;
import app.security.LoginRateLimitKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
class LoginRateLimitConcurrencyTest {

    private static final int PARALLEL_UPSERT_THREADS = 50;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private LoginRateLimitUpsert loginRateLimitUpsert;

    @Autowired
    private LoginRateLimitRepository loginRateLimitRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void clearBuckets() {
        transactionTemplate.executeWithoutResult(status -> loginRateLimitRepository.deleteAll());
    }

    @Test
    void parallelFirstFailuresOnSameIpKeyIncrementWithoutError() throws Exception {
        String ip = "198.51.100.77";
        String storageKey = LoginRateLimitKeys.storageKey("ip:", ip);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowNotBefore = now.minusSeconds(3600);
        CountDownLatch ready = new CountDownLatch(PARALLEL_UPSERT_THREADS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_UPSERT_THREADS);
        try {
            for (int i = 0; i < PARALLEL_UPSERT_THREADS; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        loginRateLimitUpsert.upsertIncrement(
                                java.util.UUID.randomUUID(), storageKey, now, windowNotBefore);
                    } catch (Throwable t) {
                        failure.compareAndSet(null, t);
                    }
                });
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            pool.shutdown();
            assert pool.awaitTermination(60, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        assertNull(failure.get(), () -> "Unexpected failure: " + failure.get());

        var bucket = transactionTemplate.execute(
                status -> loginRateLimitRepository.findByRateKey(storageKey).orElseThrow());
        assertEquals(PARALLEL_UPSERT_THREADS, bucket.getAttemptCount());
    }
}
