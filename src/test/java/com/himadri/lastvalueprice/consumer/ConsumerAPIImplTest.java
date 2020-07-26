package com.himadri.lastvalueprice.consumer;

import com.himadri.lastvalueprice.db.DataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

class ConsumerAPIImplTest {
    private static final String TEST_ID = "ID";
    @Mock
    private DataStore dataStore;

    private ConsumerAPI consumer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        consumer = new ConsumerAPIImpl(dataStore);
    }

    @Test
    void testLatestPrice() {
        when(dataStore.getPriceRecord(TEST_ID)).thenReturn(new DataStore.PriceRecord(1, new BigDecimal(2)));
        BigDecimal latestPrice = consumer.getLatestPrice(TEST_ID);
        assertEquals(new BigDecimal(2), latestPrice);
    }

    @Test
    void testNullPrice() {
        when(dataStore.getPriceRecord(TEST_ID)).thenReturn(null);
        BigDecimal latestPrice = consumer.getLatestPrice(TEST_ID);
        assertNull(latestPrice);
    }
}