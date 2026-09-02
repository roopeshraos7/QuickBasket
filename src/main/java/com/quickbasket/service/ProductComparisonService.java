package com.quickbasket.service;

import com.quickbasket.dto.BestOption;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.ProductSearchResponse;
import com.quickbasket.service.cache.ProviderSliceCacheService;
import com.quickbasket.service.provider.ProductProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service encapsulating multi-provider product search, concurrent execution via Java 21 Virtual Threads,
 * per-provider Redis slice caching, provider timeout and failure isolation, catalog persistence, and
 * dynamic price/ETA comparison logic.
 */
@Service
public class ProductComparisonService {

    private static final Logger log = LoggerFactory.getLogger(ProductComparisonService.class);

    private final List<ProductProvider> providers;
    private final ProductCatalogService catalogService;
    private final ProviderSliceCacheService sliceCacheService;
    private final Executor executor;
    private final String activeProviderCode;

    // Helper record tracking slice offers and cache hit status
    private record ProviderSliceResult(List<NormalizedProductOffer> offers, boolean isCacheHit) {}

    @Autowired
    public ProductComparisonService(
            List<ProductProvider> providers,
            ProductCatalogService catalogService,
            ProviderSliceCacheService sliceCacheService,
            @Autowired(required = false) Executor executor,
            @Value("${quickcommerce.api.active-provider:all}") String activeProviderCode
    ) {
        this.providers = providers;
        this.catalogService = catalogService;
        this.sliceCacheService = sliceCacheService;
        this.executor = executor != null ? executor : Executors.newVirtualThreadPerTaskExecutor();
        this.activeProviderCode = activeProviderCode;
    }

    // Simplified constructor for test contexts without Spring DI
    public ProductComparisonService(
            List<ProductProvider> providers,
            ProductCatalogService catalogService,
            ProviderSliceCacheService sliceCacheService,
            String activeProviderCode
    ) {
        this(providers, catalogService, sliceCacheService, null, activeProviderCode);
    }

    // Legacy test constructor compatibility
    public ProductComparisonService(
            List<ProductProvider> providers,
            ProductCatalogService catalogService,
            String activeProviderCode
    ) {
        this(providers, catalogService, null, null, activeProviderCode);
    }

    /**
     * Search products concurrently across active providers using Java 21 Virtual Threads and per-provider
     * Redis slice caching. Aggregate results, handle isolated timeouts and failures, persist fresh offer
     * snapshots to PostgreSQL, and compute dynamic best price/ETA options.
     *
     * @param query     Product search query
     * @param latitude  User location latitude
     * @param longitude User location longitude
     * @return ProductSearchResponse containing aggregated offers and best option analysis
     */
    public ProductSearchResponse searchProducts(String query, String latitude, String longitude) {
        List<ProductProvider> targetProviders = resolveTargetProviders(activeProviderCode);
        log.info("Executing concurrent product search across {} active providers for query '{}'", targetProviders.size(), query);

        if (targetProviders.isEmpty()) {
            log.warn("No active ProductProviders found for selection code '{}'.", activeProviderCode);
            return new ProductSearchResponse(query, 0, new BestOption(null, null, null, null), List.of(), List.of());
        }

        List<String> failedProviders = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<ProviderSliceResult>> futures = targetProviders.stream()
                .map(provider -> CompletableFuture.supplyAsync(() -> fetchProviderSlice(provider, query, latitude, longitude), executor)
                        .orTimeout(provider.getTimeoutMs(), TimeUnit.MILLISECONDS)
                        .exceptionally(throwable -> {
                            String code = getProviderCodeSafe(provider);
                            log.warn("Provider '{}' failed or timed out during search: {}", code, throwable.getMessage());
                            failedProviders.add(code);
                            return new ProviderSliceResult(List.of(), false);
                        }))
                .toList();

        // Wait for all provider executions to settle
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<ProviderSliceResult> sliceResults = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList();

        // Separate fresh offers from cache misses for database persistence
        List<NormalizedProductOffer> freshOffersToPersist = sliceResults.stream()
                .filter(result -> !result.isCacheHit())
                .map(ProviderSliceResult::offers)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();

        if (!freshOffersToPersist.isEmpty()) {
            catalogService.saveOffers(query, freshOffersToPersist);
        }

        // Aggregate all offers (hits + fresh misses)
        List<NormalizedProductOffer> aggregatedOffers = sliceResults.stream()
                .map(ProviderSliceResult::offers)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();

        BestOption bestOption = calculateBestOption(aggregatedOffers);

        return new ProductSearchResponse(
                query,
                aggregatedOffers.size(),
                bestOption,
                aggregatedOffers,
                List.copyOf(failedProviders)
        );
    }

    private ProviderSliceResult fetchProviderSlice(ProductProvider provider, String query, String latitude, String longitude) {
        String providerCode = getProviderCodeSafe(provider);

        if (sliceCacheService != null) {
            Optional<List<NormalizedProductOffer>> cachedSlice = sliceCacheService.getSlice(providerCode, query, latitude, longitude);
            if (cachedSlice.isPresent()) {
                return new ProviderSliceResult(cachedSlice.get(), true);
            }
        }

        // Cache MISS - Invoke provider API
        List<NormalizedProductOffer> freshOffers = provider.searchProducts(query, latitude, longitude);
        if (freshOffers == null) {
            freshOffers = List.of();
        }

        // Put fresh slice into Redis cache if cache service is available
        if (sliceCacheService != null) {
            sliceCacheService.putSlice(providerCode, query, latitude, longitude, freshOffers);
        }

        return new ProviderSliceResult(freshOffers, false);
    }

    private List<ProductProvider> resolveTargetProviders(String selectionCode) {
        if ("all".equalsIgnoreCase(selectionCode) || selectionCode == null || selectionCode.isBlank()) {
            return providers.stream()
                    .filter(ProductProvider::isEnabled)
                    .toList();
        }

        List<ProductProvider> matched = providers.stream()
                .filter(ProductProvider::isEnabled)
                .filter(p -> p.supports(selectionCode) || selectionCode.equalsIgnoreCase(getProviderCodeSafe(p)))
                .toList();

        if (matched.isEmpty()) {
            log.warn("No enabled provider matched requested code '{}'. Falling back to all enabled providers.", selectionCode);
            return providers.stream()
                    .filter(ProductProvider::isEnabled)
                    .toList();
        }

        return matched;
    }

    private String getProviderCodeSafe(ProductProvider provider) {
        try {
            String code = provider.getProviderCode();
            return code != null ? code : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private BestOption calculateBestOption(List<NormalizedProductOffer> offers) {
        if (offers == null || offers.isEmpty()) {
            return new BestOption(null, null, null, null);
        }

        List<NormalizedProductOffer> inStockOffers = offers.stream()
                .filter(NormalizedProductOffer::inStock)
                .toList();

        if (inStockOffers.isEmpty()) {
            return new BestOption(null, null, null, null);
        }

        NormalizedProductOffer cheapest = inStockOffers.stream()
                .min(Comparator.comparing(NormalizedProductOffer::price))
                .orElse(inStockOffers.get(0));

        NormalizedProductOffer fastest = inStockOffers.stream()
                .filter(o -> o.etaMinutes() != null)
                .min(Comparator.comparing(NormalizedProductOffer::etaMinutes))
                .orElse(inStockOffers.stream()
                        .filter(o -> o.delivery() != null && o.delivery().etaMinutes() != null)
                        .findFirst()
                        .orElse(cheapest));

        Integer fastestEta = fastest.etaMinutes() != null ? fastest.etaMinutes() :
                (fastest.delivery() != null ? fastest.delivery().etaMinutes() : null);

        return new BestOption(
                cheapest.platformCode(),
                cheapest.price(),
                fastest.platformCode(),
                fastestEta
        );
    }
}
