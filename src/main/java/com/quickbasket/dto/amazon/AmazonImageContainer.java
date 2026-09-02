package com.quickbasket.dto.amazon;

/**
 * Image size variants container from Amazon Creators API.
 */
public record AmazonImageContainer(
        AmazonImage small,
        AmazonImage medium,
        AmazonImage large
) {}
