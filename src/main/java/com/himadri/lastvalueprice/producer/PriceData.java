package com.himadri.lastvalueprice.producer;

import lombok.Data;

@Data
public class PriceData {
    private final String id;
    private final long asOf;
    private final PricePayload pricePayload;
}
