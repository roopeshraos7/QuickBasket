package com.quickbasket.dto.amazon;

/**
 * Price and saving basis container from Amazon Creators API.
 */
public record AmazonPrice(
        AmazonMoney price,
        AmazonMoney savingBasis,
        Double savingsPercentage
) {}
