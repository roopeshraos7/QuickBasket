package com.quickbasket.dto.amazon;

/**
 * Stock availability container from Amazon Creators API.
 */
public record AmazonAvailability(
        String type,
        String message,
        Integer maxOrderQuantity
) {}
