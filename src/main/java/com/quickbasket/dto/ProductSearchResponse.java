package com.quickbasket.dto;

import java.util.List;

/**
 * Top-level response structure for product search operations.
 *
 * @param query           Search query text
 * @param totalResults    Total number of offers aggregated across active providers
 * @param bestOption      Cheapest and fastest offer recommendations
 * @param offers          Aggregated list of normalized product offers
 * @param failedProviders List of provider codes that timed out or failed during execution (empty if all succeeded)
 */
public record ProductSearchResponse(
        String query,
        int totalResults,
        BestOption bestOption,
        List<NormalizedProductOffer> offers,
        List<String> failedProviders
) {
    /**
     * Backward-compatible constructor for calls omitting failedProviders.
     */
    public ProductSearchResponse(
            String query,
            int totalResults,
            BestOption bestOption,
            List<NormalizedProductOffer> offers
    ) {
        this(query, totalResults, bestOption, offers, List.of());
    }
}
