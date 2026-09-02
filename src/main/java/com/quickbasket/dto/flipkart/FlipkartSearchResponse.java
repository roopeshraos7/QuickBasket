package com.quickbasket.dto.flipkart;

import java.util.List;

/**
 * Top-level JSON search response returned by official Flipkart Affiliate v1.0 API (/affiliate/1.0/search.json).
 */
public record FlipkartSearchResponse(
        List<FlipkartProductWrapper> productInfoList
) {}
