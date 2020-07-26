package com.himadri.lastvalueprice.producer;

import java.math.BigDecimal;

public interface PricePayload {
    BigDecimal getPrice();
}
