package com.himadri.lastvalueprice.consumer;

import java.math.BigDecimal;

public interface ConsumerAPI {
    BigDecimal getLatestPrice(String id);
}
