package com.quickbasket.dto.flipkart;

/**
 * Container wrapping product identifier and product attributes.
 */
public record FlipkartProductBaseInfo(
        FlipkartProductIdentifier productIdentifier,
        FlipkartProductAttributes productAttributes
) {}
