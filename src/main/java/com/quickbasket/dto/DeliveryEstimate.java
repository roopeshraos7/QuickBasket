package com.quickbasket.dto;

import java.math.BigDecimal;

/**
 * Encapsulates delivery timelines, fulfillment mode, and shipping fee estimates.
 *
 * @param type         Fulfillment speed type (INSTANT, EXPRESS, STANDARD)
 * @param etaMinutes   Estimated delivery duration in minutes (may be null for standard e-commerce multi-day shipping)
 * @param deliveryText Human-readable timeline text (e.g., "In 10 mins", "Tomorrow by 2 PM", "Delivery in 3-5 days")
 * @param shippingFee  Fulfillment/shipping cost as BigDecimal (may be zero for free delivery)
 */
public record DeliveryEstimate(
        DeliveryType type,
        Integer etaMinutes,
        String deliveryText,
        BigDecimal shippingFee
) {
    public static DeliveryEstimate instant(int minutes) {
        return new DeliveryEstimate(
                DeliveryType.INSTANT,
                minutes,
                "In " + minutes + " mins",
                BigDecimal.ZERO
        );
    }
}
