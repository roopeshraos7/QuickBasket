package com.quickbasket.dto;

import java.math.BigDecimal;

/**
 * Immutable canonical representation of a product offer from a quick-commerce platform.
 *
 * @param platformCode       Unique identifier of the platform (e.g. "BLINKIT", "ZEPTO", "INSTAMART")
 * @param platformName       Human-readable name of the platform (e.g. "Blinkit", "Zepto", "Swiggy Instamart")
 * @param price              Current selling price in INR
 * @param mrp                Maximum Retail Price in INR
 * @param discountPercentage Calculated discount percentage
 * @param inStock            Availability status
 * @param etaMinutes         Estimated delivery time in minutes
 * @param productUrl         Direct link to the product on the vendor platform
 * @param imageUrl           Product thumbnail image URL
 */
public record NormalizedProductOffer(
        String platformCode,
        String platformName,
        BigDecimal price,
        BigDecimal mrp,
        BigDecimal discountPercentage,
        boolean inStock,
        Integer etaMinutes,
        String productUrl,
        String imageUrl
) {}
