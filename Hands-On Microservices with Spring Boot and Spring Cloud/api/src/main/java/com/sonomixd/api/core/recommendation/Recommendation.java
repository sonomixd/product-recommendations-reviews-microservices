package com.sonomixd.api.core.recommendation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class Recommendation {
    private final int productId;
    private final int recommendationId;
    private final String author;
    private final int rate;
    private final String content;
    private final String serviceAddress;

    public Recommendation() {
        productId = 0;
        recommendationId = 0;
        author = null;
        rate = 0;
        content = null;
        serviceAddress = null;
    }
}

