package com.quickbasket.controller;

import com.quickbasket.dto.ProductSearchResponse;
import com.quickbasket.service.ProductComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing product search and price comparison endpoints.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Product Comparison", description = "Endpoints for searching and comparing products across quick-commerce platforms")
public class ProductComparisonController {

    private final ProductComparisonService searchService;

    public ProductComparisonController(ProductComparisonService searchService) {
        this.searchService = searchService;
    }

    /**
     * Search product offers across platforms.
     *
     * @param query Search query term (e.g. "Amul Taaza Milk 1L")
     * @param lat   Latitude coordinate (optional, default: 12.9716)
     * @param lng   Longitude coordinate (optional, default: 77.5946)
     * @return ProductSearchResponse containing offers and best option analysis
     */
    @GetMapping("/search")
    @Operation(summary = "Search product offers", description = "Aggregates, normalizes, and compares product offers across platforms for a query and location")
    public ProductSearchResponse searchProducts(
            @Parameter(description = "Product search query term", required = true, example = "Amul Taaza Milk 1L")
            @RequestParam(name = "q") String query,

            @Parameter(description = "User latitude coordinate", example = "12.9716")
            @RequestParam(name = "lat", defaultValue = "12.9716") String lat,

            @Parameter(description = "User longitude coordinate", example = "77.5946")
            @RequestParam(name = "lng", defaultValue = "77.5946") String lng
    ) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query parameter 'q' cannot be blank.");
        }
        return searchService.searchProducts(query.trim(), lat, lng);
    }
}
