package com.quickbasket.dto;

import java.math.BigDecimal;

/**
 * Summary record highlighting the cheapest and fastest quick-commerce options for a search query.
 *
 * @param cheapestPlatformCode Platform code offering the lowest price
 * @param cheapestPrice        Lowest price available among in-stock offers
 * @param fastestPlatformCode  Platform code offering the shortest ETA
 * @param fastestEtaMinutes    Shortest estimated delivery time in minutes among in-stock offers
 */
public record BestOption(
        String cheapestPlatformCode,
        BigDecimal cheapestPrice,
        String fastestPlatformCode,
        Integer fastestEtaMinutes
) {}
