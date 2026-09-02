package com.quickbasket.dto.amazon;

import java.util.List;

/**
 * Search result container wrapping items list from Amazon Creators API.
 */
public record AmazonSearchResult(
        Integer totalResultCount,
        List<AmazonItem> items
) {}
