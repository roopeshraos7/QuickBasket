package com.quickbasket.dto.amazon;

/**
 * Product item container from Amazon Creators API SearchItems response.
 */
public record AmazonItem(
        String asin,
        String detailPageURL,
        AmazonImages images,
        AmazonItemInfo itemInfo,
        AmazonOffersV2 offersV2
) {}
