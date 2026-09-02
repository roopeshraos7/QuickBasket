package com.quickbasket.dto.amazon;

/**
 * Offer listing container from Amazon Creators API OffersV2.
 */
public record AmazonListing(
        String id,
        Boolean isBuyBoxWinner,
        AmazonPrice price,
        AmazonMoney savingBasis,
        AmazonAvailability availability,
        AmazonMerchantInfo merchantInfo
) {}
