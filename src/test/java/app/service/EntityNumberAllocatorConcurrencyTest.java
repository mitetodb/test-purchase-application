package app.service;

import app.TestPurchaseApplication;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
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

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
class EntityNumberAllocatorConcurrencyTest {

    private static final int PARALLEL_THREADS = 50;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private EntityNumberAllocator entityNumberAllocator;

    @Autowired
    private TestPurchaseTestDataCleaner testPurchaseTestDataCleaner;

    @BeforeEach
    void resetSequences() {
        testPurchaseTestDataCleaner.resetEntityNumberSequencesToDefaults();
    }

    @Test
    void parallelCustomerNumbersAreUnique() throws Exception {
        Set<String> numbers = runParallel(() -> entityNumberAllocator.nextCustomerNumber());
        assertEquals(PARALLEL_THREADS, numbers.size());
        assertTrue(numbers.contains("0001"));
        assertTrue(numbers.contains(String.format("%04d", PARALLEL_THREADS)));
    }

    @Test
    void parallelShopNumbersAreUnique() throws Exception {
        Set<String> numbers = runParallel(() -> entityNumberAllocator.nextShopNumber());
        assertEquals(PARALLEL_THREADS, numbers.size());
    }

    @Test
    void parallelTestPurchaseNumbersAreUnique() throws Exception {
        Set<String> numbers = runParallel(() -> entityNumberAllocator.nextTestPurchaseNumber());
        assertEquals(PARALLEL_THREADS, numbers.size());
        assertTrue(numbers.stream().allMatch(n -> n.startsWith("TP-")));
        assertTrue(numbers.contains("TP-1001"));
        assertTrue(numbers.contains("TP-" + String.format("%04d", 1000 + PARALLEL_THREADS)));
    }

    private Set<String> runParallel(NumberSupplier supplier) throws Exception {
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
                        numbers.add(supplier.next());
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
        return new HashSet<>(numbers);
    }

    @FunctionalInterface
    private interface NumberSupplier {
        String next();
    }
}
