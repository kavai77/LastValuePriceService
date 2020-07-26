package com.himadri.lastvalueprice.producer;

import java.util.List;

public interface ProducerAPI {
    BatchId startBatch();

    void uploadBatch(BatchId batchId, List<PriceData> priceDataStream);

    void commitBatch(BatchId batchId) throws InterruptedException;

    void cancelBatch(BatchId batchId);
}
