package com.quickbasket.dto.flipkart;

/**
 * Container wrapping v1 product identifier and product attributes from Flipkart Affiliate v1.0 API.
 */
public record FlipkartProductBaseInfoV1(
        FlipkartProductIdentifier productIdentifier,
        FlipkartProductAttributes productAttributes
) {}
