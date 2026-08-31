package com.quickbasket.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Raw DTO record mapping top-level search responses returned by QuickCommerce API.
 */
public record QuickCommerceRawResponse(
        @JsonProperty("query") String query,
        @JsonProperty("offers") List<QuickCommerceRawOffer> offers
) {}
