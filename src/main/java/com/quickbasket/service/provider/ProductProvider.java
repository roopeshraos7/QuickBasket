package com.quickbasket.service.provider;

import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;

import java.util.List;

/**
 * Common Strategy pattern interface for all product providers (quick-commerce and e-commerce).
 */
public interface ProductProvider {

    /**
     * Executes product search against the provider.
     *
     * @param query     Search query term
     * @param latitude  User location latitude
     * @param longitude User location longitude
     * @return List of normalized product offers
     */
    List<NormalizedProductOffer> searchProducts(String query, String latitude, String longitude);

    /**
     * Checks if this provider handles the requested provider code or category selection.
     */
    boolean supports(String providerCode);

    /**
     * Unique identifier code for the provider (e.g. MOCK, QUICKCOMMERCE, AMAZON, FLIPKART).
     */
    default String getProviderCode() {
        return "DEFAULT";
    }

    /**
     * Exposes the platform type category (QUICK_COMMERCE vs ECOMMERCE).
     */
    default PlatformType getPlatformType() {
        return PlatformType.QUICK_COMMERCE;
    }

    /**
     * Availability flag indicating whether provider is enabled via configuration.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Configurable execution timeout in milliseconds for this provider.
     */
    default long getTimeoutMs() {
        return 1500L;
    }
}
