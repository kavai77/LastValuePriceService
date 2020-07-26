package com.himadri.lastvalueprice.producer;

import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

@Data
public class BatchId {
    private static final AtomicLong batchIdGenerator = new AtomicLong();

    private final long id;

    private BatchId(long id) {
        this.id = id;
    }

    public static BatchId createNewBatchId(){
        return new BatchId(batchIdGenerator.getAndIncrement());
    }
}
