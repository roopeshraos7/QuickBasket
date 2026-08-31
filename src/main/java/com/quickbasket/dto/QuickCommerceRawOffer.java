package com.quickbasket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * Raw DTO record mapping individual platform offer payloads returned by QuickCommerce API.
 */
public record QuickCommerceRawOffer(
        @JsonProperty("platform_code") String platformCode,
        @JsonProperty("platform_name") String platformName,
        @JsonProperty("price") BigDecimal price,
        @JsonProperty("mrp") BigDecimal mrp,
        @JsonProperty("in_stock") Boolean inStock,
        @JsonProperty("eta_minutes") Integer etaMinutes,
        @JsonProperty("product_url") String productUrl,
        @JsonProperty("image_url") String imageUrl
) {}
