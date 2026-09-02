package com.quickbasket.dto.flipkart;

/**
 * Product identifier container wrapping external item SKU from Flipkart Affiliate API.
 */
public record FlipkartProductIdentifier(
        String productId
) {}
