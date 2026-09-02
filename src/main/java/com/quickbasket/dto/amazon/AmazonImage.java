package com.quickbasket.dto.amazon;

/**
 * Image URL and dimensions container from Amazon Creators API.
 */
public record AmazonImage(
        String url,
        Integer height,
        Integer width
) {}
