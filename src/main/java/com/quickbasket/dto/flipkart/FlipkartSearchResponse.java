package com.quickbasket.dto.flipkart;

import java.util.List;

/**
 * Top-level JSON search response returned by official Flipkart Affiliate API.
 */
public record FlipkartSearchResponse(
        List<FlipkartProductWrapper> products
) {}
