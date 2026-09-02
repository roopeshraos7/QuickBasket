package com.quickbasket.dto.flipkart;

/**
 * Product item container wrapping product base info and shipping info.
 */
public record FlipkartProductWrapper(
        FlipkartProductBaseInfo productBaseInfo,
        FlipkartShippingInfo shippingInfo
) {}
