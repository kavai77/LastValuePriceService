package com.himadri.lastvalueprice.consumer;

import com.himadri.lastvalueprice.db.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ConsumerAPIImpl implements ConsumerAPI {
    private final DataStore dataStore;

    @Autowired
    public ConsumerAPIImpl(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Override
    public BigDecimal getLatestPrice(String id) {
        DataStore.PriceRecord priceRecord = dataStore.getPriceRecord(id);
        if (priceRecord == null) {
            return null;
        }
        return priceRecord.getPrice();
    }
}
