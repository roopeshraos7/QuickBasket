package com.quickbasket.dto.flipkart;

import java.util.Map;

/**
 * Product detail attributes returned by Flipkart Affiliate v1.0 API.
 */
public record FlipkartProductAttributes(
        String title,
        String productBrand,
        FlipkartPriceInfo sellingPrice,
        FlipkartPriceInfo maximumRetailPrice,
        Double discountPercentage,
        Boolean inStock,
        Boolean isAvailable,
        String productUrl,
        Map<String, String> imageUrls,
        String sellerName,
        FlipkartShippingInfo shippingInfo
) {}
