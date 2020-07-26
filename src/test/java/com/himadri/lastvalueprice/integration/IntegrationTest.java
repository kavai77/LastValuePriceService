package com.himadri.lastvalueprice.integration;

import com.himadri.lastvalueprice.consumer.ConsumerAPI;
import com.himadri.lastvalueprice.consumer.ConsumerAPIImpl;
import com.himadri.lastvalueprice.db.DataStore;
import com.himadri.lastvalueprice.producer.BatchId;
import com.himadri.lastvalueprice.producer.PriceData;
import com.himadri.lastvalueprice.producer.ProducerAPI;
import com.himadri.lastvalueprice.producer.ProducerAPIImpl;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(classes = {ProducerAPIImpl.class, ConsumerAPIImpl.class, DataStore.class})
public class IntegrationTest {
    @Autowired
    private ProducerAPI producerAPI;

    @Autowired
    private ConsumerAPI consumerAPI;

    @Test
    void testNormalLifeCycle() {
        BatchId batchId = producerAPI.startBatch();
        String id = "id";
        producerAPI.uploadBatch(batchId, List.of(new PriceData(id, 1, () -> new BigDecimal(1))));

        // no updates before commit
        assertNull(consumerAPI.getLatestPrice(id));

        producerAPI.commitBatch(batchId);
        assertEquals(new BigDecimal(1), consumerAPI.getLatestPrice(id));
    }


    /**
     * This test updates the prices with multiple producers and multiple batches concurrently.
     * By the end, the atomic counter should hold the final value for assertions
     */
    @Test
    public void loadTestUpdates() throws Exception {
        Random random = new Random();

        final int concurrentProducerCount = 100;
        final int batchCount = 50;
        final int numberOfTickers = 10;

        List<IdAndCounter> idsAndCounters = IntStream.range(0, numberOfTickers)
            .mapToObj(i -> new IdAndCounter("id" + i))
            .collect(Collectors.toList());

        ExecutorService executorService = Executors.newCachedThreadPool();
        CountDownLatch concurrentProducerCountDown = new CountDownLatch(concurrentProducerCount);
        for (int i = 0; i < concurrentProducerCount; i++) {
            executorService.submit(() -> {
                BatchId batchId = producerAPI.startBatch();

                CountDownLatch batchCountDown = new CountDownLatch(batchCount);
                for (int j = 0; j < batchCount; j++) {
                    executorService.submit(() -> {
                        List<PriceData> priceData = new ArrayList<>();
                        for (IdAndCounter idAndCounter: idsAndCounters) {
                            if (random.nextInt() % 4 == 0) { // 25% chance that we send an update
                                long timeAndPrice = idAndCounter.getCounter().getAndIncrement();
                                priceData.add(new PriceData(idAndCounter.getId(), timeAndPrice, () -> new BigDecimal(timeAndPrice)));
                            }
                        }
                        producerAPI.uploadBatch(batchId, priceData);
                        batchCountDown.countDown();
                    });
                }
                executorService.submit(() -> {
                    try {
                        batchCountDown.await();
                        producerAPI.commitBatch(batchId);
                    } catch (InterruptedException ignored) {}
                });
                concurrentProducerCountDown.countDown();
            });
        }
        concurrentProducerCountDown.await();
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        for (IdAndCounter idAndCounter: idsAndCounters) {
            BigDecimal expectedLatestPrice = new BigDecimal(idAndCounter.getCounter().get() - 1);
            assertEquals(expectedLatestPrice, consumerAPI.getLatestPrice(idAndCounter.getId()));
        }
    }

    @Data
    private static class IdAndCounter {
        private final String id;
        private final AtomicLong counter = new AtomicLong();
    }
}
