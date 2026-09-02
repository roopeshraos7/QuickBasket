package com.quickbasket.dto.flipkart;

/**
 * Shipping fee and estimated delivery timeline container from Flipkart Affiliate API.
 */
public record FlipkartShippingInfo(
        FlipkartPriceInfo shippingFees,
        String estimatedDelivery
) {}
