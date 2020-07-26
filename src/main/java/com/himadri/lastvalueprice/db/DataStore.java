package com.himadri.lastvalueprice.db;

import com.himadri.lastvalueprice.producer.PriceData;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DataStore {
    // no need to use ConcurrentHashMap: this map is read-only concurrently, and it is never updated
    // we always create a new hashmap object upon price update
    private final AtomicReference<Map<String, PriceRecord>> latestPriceStore = new AtomicReference<>(new HashMap<>());
    private final Semaphore updatePriceSemaphore = new Semaphore(1);

    public PriceRecord getPriceRecord(String id) {
        return latestPriceStore.get().get(id);
    }

    public void updatePrices(List<PriceData> priceDataList) {
        // only one concurrent batch update is allowed: we optimize for reads over writes
        updatePriceSemaphore.acquireUninterruptibly();
        try {
            // we prepare the new prices by copying the existing ones
            Map<String, PriceRecord> newPrices = new HashMap<>(latestPriceStore.get());
            for (PriceData priceData: priceDataList) {
                PriceRecord currentPriceRecord = newPrices.get(priceData.getId());
                if (currentPriceRecord == null || currentPriceRecord.getAsOf() < priceData.getAsOf()) {
                    PriceRecord newPriceRecord = new PriceRecord(priceData.getAsOf(), priceData.getPricePayload().getPrice());
                    newPrices.put(priceData.getId(), newPriceRecord);
                }
            }

            // we replace the new prices once it is prepared
            latestPriceStore.set(newPrices);
        } finally {
            updatePriceSemaphore.release();
        }
    }

    @Data
    public static class PriceRecord {
        private final long asOf;
        private final BigDecimal price;
    }
}
