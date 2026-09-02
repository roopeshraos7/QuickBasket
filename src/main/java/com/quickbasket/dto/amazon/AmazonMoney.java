package com.quickbasket.dto.amazon;

import java.math.BigDecimal;

/**
 * Monetary amount and currency container from Amazon Creators API.
 */
public record AmazonMoney(
        BigDecimal amount,
        String currency
) {}
