package com.quickbasket.dto;

import java.util.List;

/**
 * REST API response record for product comparison searches (GET /api/v1/products/search).
 *
 * @param query        The original search query term
 * @param totalResults Total number of product offers returned
 * @param bestOption   Calculated summary of cheapest and fastest options
 * @param offers       List of normalized product offers across platforms
 */
public record ProductSearchResponse(
        String query,
        Integer totalResults,
        BestOption bestOption,
        List<NormalizedProductOffer> offers
) {}
