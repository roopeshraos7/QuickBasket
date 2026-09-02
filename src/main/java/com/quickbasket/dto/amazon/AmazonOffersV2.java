package com.quickbasket.dto.amazon;

import java.util.List;

/**
 * Container wrapping offer listings from Amazon Creators API OffersV2.
 */
public record AmazonOffersV2(
        List<AmazonListing> listings
) {}
