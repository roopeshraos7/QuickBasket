package com.quickbasket.dto;

import java.math.BigDecimal;

/**
 * Normalized product offer DTO across all e-commerce and quick-commerce platforms.
 *
 * @param platformCode       Unique platform identifier (e.g. BLINKIT, ZEPTO, AMAZON)
 * @param platformName       Human-readable platform name (e.g. Blinkit, Zepto, Amazon India)
 * @param platformType       Classification enum (QUICK_COMMERCE vs ECOMMERCE)
 * @param price              Current selling price
 * @param mrp                Maximum Retail Price
 * @param discountPercentage Discount percentage relative to MRP
 * @param inStock            Availability flag
 * @param delivery           Structured delivery timelines and shipping fee
 * @param sellerName         Seller/Merchant name (e.g., "Appario Retail", "Blinkit Commerce")
 * @param productUrl         Direct platform product URL
 * @param imageUrl           Product image URL
 */
public record NormalizedProductOffer(
        String platformCode,
        String platformName,
        PlatformType platformType,
        BigDecimal price,
        BigDecimal mrp,
        BigDecimal discountPercentage,
        boolean inStock,
        DeliveryEstimate delivery,
        String sellerName,
        String productUrl,
        String imageUrl
) {

    /**
     * Backward-compatible constructor for Phase 1-3 instant delivery callers.
     */
    public NormalizedProductOffer(
            String platformCode,
            String platformName,
            BigDecimal price,
            BigDecimal mrp,
            BigDecimal discountPercentage,
            boolean inStock,
            Integer etaMinutes,
            String productUrl,
            String imageUrl
    ) {
        this(
                platformCode,
                platformName,
                PlatformType.QUICK_COMMERCE,
                price,
                mrp,
                discountPercentage,
                inStock,
                etaMinutes != null ? DeliveryEstimate.instant(etaMinutes) : null,
                null,
                productUrl,
                imageUrl
        );
    }

    /**
     * Helper method to extract ETA in minutes for backwards-compatibility and comparison calculations.
     */
    public Integer etaMinutes() {
        return delivery != null ? delivery.etaMinutes() : null;
    }
}
