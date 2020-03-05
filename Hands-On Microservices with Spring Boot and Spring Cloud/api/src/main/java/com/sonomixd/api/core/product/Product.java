package com.sonomixd.api.core.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Product {

    private Integer productId;
    private String name;
    private Integer weight;
    private String serviceAddress;
}
