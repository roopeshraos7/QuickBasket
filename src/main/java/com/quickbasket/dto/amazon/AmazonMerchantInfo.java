package com.quickbasket.dto.amazon;

/**
 * Merchant / seller information container from Amazon Creators API.
 */
public record AmazonMerchantInfo(
        String id,
        String name
) {}
