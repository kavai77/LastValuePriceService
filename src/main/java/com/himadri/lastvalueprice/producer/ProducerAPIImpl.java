package com.himadri.lastvalueprice.producer;

import com.himadri.lastvalueprice.db.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class ProducerAPIImpl implements ProducerAPI {
    public static final int BATCH_MAX_SIZE = 1000;

    private final DataStore dataStore;
    private final ConcurrentMap<BatchId, List<PriceData>> batchStorage = new ConcurrentHashMap<>();

    @Autowired
    public ProducerAPIImpl(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public BatchId startBatch() {
        BatchId newBatchId = BatchId.createNewBatchId();
        List<PriceData> oldBatch = batchStorage.putIfAbsent(newBatchId, new ArrayList<>());
        if (oldBatch != null) {
            throw new RuntimeException("BatchId creation not atomic. This should never happen!");
        }
        return newBatchId;
    }

    @Override
    public void uploadBatch(BatchId batchId, List<PriceData> batchPriceData) {
        if (batchPriceData.size() > BATCH_MAX_SIZE) {
            throw new IllegalArgumentException("Batch size too large: " + batchPriceData.size());
        }
        // uploading batches can be invoked concurrently
        // there is no performance penalty for different producers to upload batches with different batchids
        // however, in reality one producer is invoking updateBatch in sequence, therefore here it is ok to block for batchId
        synchronized (batchId) {
            List<PriceData> priceData = batchStorage.get(batchId);
            if (priceData == null) {
                throw new BatchIdNotFoundException();
            }

            // flatten the batch data into a single list
            priceData.addAll(batchPriceData);
        }
    }

    @Override
    public void commitBatch(BatchId batchId) {
        List<PriceData> priceData;
        synchronized (batchId) {
            priceData = batchStorage.remove(batchId);
            if (priceData == null) {
                throw new BatchIdNotFoundException();
            }
        }
        dataStore.updatePrices(priceData);
    }

    @Override
    public void cancelBatch(BatchId batchId) {
        synchronized (batchId) {
            if (batchStorage.remove(batchId) == null){
                throw new BatchIdNotFoundException();
            }
        }
    }
}
