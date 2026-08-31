package com.quickbasket.service.provider;

import com.quickbasket.dto.NormalizedProductOffer;
import java.util.List;

/**
 * Strategy pattern interface for quick-commerce product data providers.
 * Decouples core business logic from third-party vendor APIs.
 */
public interface ProductProvider {

    /**
     * Search products across quick-commerce platforms for a given query and location.
     *
     * @param query     Product search query term (e.g. "Amul Taaza Milk 1L")
     * @param latitude  User location latitude coordinate
     * @param longitude User location longitude coordinate
     * @return List of normalized product offers from supported vendor platforms
     */
    List<NormalizedProductOffer> searchProducts(String query, String latitude, String longitude);

    /**
     * Determines whether this provider supports the requested provider code.
     *
     * @param providerCode Identifier code (e.g. "mock", "quickcommerce")
     * @return true if supported, false otherwise
     */
    boolean supports(String providerCode);
}
