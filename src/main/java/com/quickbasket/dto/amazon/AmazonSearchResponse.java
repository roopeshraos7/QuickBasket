package com.quickbasket.dto.amazon;

/**
 * Top-level JSON search response returned by Amazon Creators API SearchItems (/catalog/v1/searchItems).
 */
public record AmazonSearchResponse(
        AmazonSearchResult searchResult
) {}
