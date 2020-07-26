package com.himadri.lastvalueprice.db;

import com.himadri.lastvalueprice.producer.PriceData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataStoreTest {
    private static final String TEST_ID = "ID";

    private DataStore dataStore;

    @BeforeEach
    void setUp() {
        dataStore = new DataStore();
    }

    @Test
    public void testTransactionalUpdates() throws Exception {
        String testId1 = "ID1";
        String testId2 = "ID2";
        String testId3 = "ID3";

        // No price data
        assertNull(dataStore.getPriceRecord(testId1));
        assertNull(dataStore.getPriceRecord(testId2));

        // Price updates succeed
        dataStore.updatePrices(List.of(
            new PriceData(testId1, 10L, () -> new BigDecimal(100)),
            new PriceData(testId2, 9L, () -> new BigDecimal(99)),
            new PriceData(testId3, 8L, () -> new BigDecimal(98))
        ));
        assertEquals(new DataStore.PriceRecord(10L, new BigDecimal(100)), dataStore.getPriceRecord(testId1));
        assertEquals(new DataStore.PriceRecord(9L, new BigDecimal(99)), dataStore.getPriceRecord(testId2));
        assertEquals(new DataStore.PriceRecord(8L, new BigDecimal(98)), dataStore.getPriceRecord(testId3));

        // Older updates ignored
        dataStore.updatePrices(List.of(
            new PriceData(testId1, 11L, () -> new BigDecimal(110)), // newer
            new PriceData(testId2, 9L, () -> new BigDecimal(120)), // same timestamp as latest -> ignored
            new PriceData(testId3, 7L, () -> new BigDecimal(130)) // older as latest -> ignored
        ));
        assertEquals(new DataStore.PriceRecord(11L, new BigDecimal(110)), dataStore.getPriceRecord(testId1));
        assertEquals(new DataStore.PriceRecord(9L, new BigDecimal(99)), dataStore.getPriceRecord(testId2));
        assertEquals(new DataStore.PriceRecord(8L, new BigDecimal(98)), dataStore.getPriceRecord(testId3));

        // Failure -> complete batch ignored
        assertThrows(RuntimeException.class, () -> dataStore.updatePrices(List.of(
            new PriceData(testId1, 12L, () -> new BigDecimal(120)),
            new PriceData(testId2, 12L, () -> new BigDecimal(130)),
            new PriceData(testId3, 12L, () -> {throw new RuntimeException();})
        )));
        assertEquals(new DataStore.PriceRecord(11L, new BigDecimal(110)), dataStore.getPriceRecord(testId1));
        assertEquals(new DataStore.PriceRecord(9L, new BigDecimal(99)), dataStore.getPriceRecord(testId2));
        assertEquals(new DataStore.PriceRecord(8L, new BigDecimal(98)), dataStore.getPriceRecord(testId3));
    }

    @Test
    public void loadTestUpdatePrice() throws Exception {
        AtomicLong counter = new AtomicLong();
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 1000; i++) {
            executorService.submit(() -> {
                long timeAndPrice = counter.getAndIncrement();
                try {
                    dataStore.updatePrices(List.of(new PriceData(TEST_ID, timeAndPrice, () -> new BigDecimal(timeAndPrice))));
                } catch (InterruptedException ignored) {}
            });
        }
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        long expectedAsOf = counter.get() - 1;
        BigDecimal expectedLatestPrice = new BigDecimal(expectedAsOf);
        assertEquals(new DataStore.PriceRecord(expectedAsOf, expectedLatestPrice), dataStore.getPriceRecord(TEST_ID));
    }

}