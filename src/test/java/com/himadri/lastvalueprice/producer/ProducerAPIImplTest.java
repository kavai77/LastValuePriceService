package com.himadri.lastvalueprice.producer;

import com.himadri.lastvalueprice.db.DataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ProducerAPIImplTest {
    @Mock
    private DataStore dataStore;

    private ProducerAPI producer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        producer = new ProducerAPIImpl(dataStore);
    }

    @Test
    public void testUploadMultipleBatches() {
        BatchId batchId = producer.startBatch();
        PriceData priceData1 = new PriceData("1", 1, () -> new BigDecimal(1));
        PriceData priceData2 = new PriceData("2", 2, () -> new BigDecimal(2));
        PriceData priceData3 = new PriceData("3", 3, () -> new BigDecimal(3));

        producer.uploadBatch(batchId, List.of(priceData1));
        producer.uploadBatch(batchId, List.of());
        producer.uploadBatch(batchId, List.of(priceData2, priceData3));
        producer.uploadBatch(batchId, List.of(priceData3));

        producer.commitBatch(batchId);

        Mockito.verify(dataStore).updatePrices(List.of(
            priceData1,
            priceData2,
            priceData3,
            priceData3
        ));
    }

    @Test
    public void testOperationsAfterCommit() {
        BatchId batchId = producer.startBatch();
        PriceData priceData1 = new PriceData("1", 1, () -> new BigDecimal(1));

        producer.uploadBatch(batchId, List.of(priceData1));

        producer.commitBatch(batchId);
        Mockito.verify(dataStore).updatePrices(List.of(priceData1));

        assertThrows(BatchIdNotFoundException.class, () -> producer.uploadBatch(batchId, List.of()));
        assertThrows(BatchIdNotFoundException.class, () -> producer.commitBatch(batchId));
        assertThrows(BatchIdNotFoundException.class, () -> producer.cancelBatch(batchId));
    }

    @Test
    public void testOperationsAfterCancel() {
        BatchId batchId = producer.startBatch();
        producer.cancelBatch(batchId);
        assertThrows(BatchIdNotFoundException.class, () -> producer.uploadBatch(batchId, List.of()));
        assertThrows(BatchIdNotFoundException.class, () -> producer.commitBatch(batchId));
        assertThrows(BatchIdNotFoundException.class, () -> producer.cancelBatch(batchId));
    }


    @Test
    public void testInvalidBatchId() {
        BatchId batchId = BatchId.createNewBatchId();
        assertThrows(BatchIdNotFoundException.class, () -> producer.uploadBatch(batchId, List.of()));
        assertThrows(BatchIdNotFoundException.class, () -> producer.commitBatch(batchId));
        assertThrows(BatchIdNotFoundException.class, () -> producer.cancelBatch(batchId));
    }

    @Test
    public void tooBigBatchSize() {
        BatchId batchId = producer.startBatch();
        List<PriceData> priceDataList = Stream
            .generate(() -> new PriceData("1", 1, () -> new BigDecimal(1)))
            .limit(ProducerAPIImpl.BATCH_MAX_SIZE + 1)
            .collect(Collectors.toList());

        assertThrows(IllegalArgumentException.class, () -> producer.uploadBatch(batchId, priceDataList));
    }
}