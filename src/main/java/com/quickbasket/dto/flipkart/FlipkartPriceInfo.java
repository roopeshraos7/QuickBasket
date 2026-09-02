package com.quickbasket.dto.flipkart;

import java.math.BigDecimal;

/**
 * Monetary amount and currency container from Flipkart Affiliate API.
 */
public record FlipkartPriceInfo(
        BigDecimal amount,
        String currency
) {}
