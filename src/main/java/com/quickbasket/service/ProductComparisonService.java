package com.quickbasket.service;

import com.quickbasket.dto.BestOption;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.ProductSearchResponse;
import com.quickbasket.service.provider.ProductProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service encapsulating multi-provider product search, concurrent execution via Java 21 Virtual Threads,
 * provider timeout and failure isolation, catalog persistence, and price/ETA comparison logic.
 */
@Service
public class ProductComparisonService {

    private static final Logger log = LoggerFactory.getLogger(ProductComparisonService.class);

    private final List<ProductProvider> providers;
    private final ProductCatalogService catalogService;
    private final Executor executor;
    private final String activeProviderCode;

    @Autowired
    public ProductComparisonService(
            List<ProductProvider> providers,
            ProductCatalogService catalogService,
            @Autowired(required = false) Executor executor,
            @Value("${quickcommerce.api.active-provider:all}") String activeProviderCode
    ) {
        this.providers = providers;
        this.catalogService = catalogService;
        this.executor = executor != null ? executor : Executors.newVirtualThreadPerTaskExecutor();
        this.activeProviderCode = activeProviderCode;
    }

    // Constructor for testing without task executor bean
    public ProductComparisonService(
            List<ProductProvider> providers,
            ProductCatalogService catalogService,
            String activeProviderCode
    ) {
        this(providers, catalogService, null, activeProviderCode);
    }

    /**
     * Search products concurrently across active providers using Java 21 Virtual Threads,
     * aggregate results, handle timeouts and isolated failures, persist offer snapshots,
     * and compute best price/ETA options.
     * Caches overall search result in Redis under 'product_searches' for 5 minutes.
     *
     * @param query     Product search query
     * @param latitude  User location latitude
     * @param longitude User location longitude
     * @return ProductSearchResponse containing aggregated offers and best option analysis
     */
    @Cacheable(value = "product_searches", key = "'qb:search:' + (#query != null ? #query.toLowerCase().trim() : '') + '_' + (#latitude != null ? #latitude : 'default') + '_' + (#longitude != null ? #longitude : 'default')")
    public ProductSearchResponse searchProducts(String query, String latitude, String longitude) {
        List<ProductProvider> targetProviders = resolveTargetProviders(activeProviderCode);
        log.info("Executing concurrent product search across {} active providers for query '{}'", targetProviders.size(), query);

        if (targetProviders.isEmpty()) {
            log.warn("No active ProductProviders found for selection code '{}'.", activeProviderCode);
            return new ProductSearchResponse(query, 0, new BestOption(null, null, null, null), List.of(), List.of());
        }

        List<String> failedProviders = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<List<NormalizedProductOffer>>> futures = targetProviders.stream()
                .map(provider -> CompletableFuture.supplyAsync(() -> provider.searchProducts(query, latitude, longitude), executor)
                        .orTimeout(provider.getTimeoutMs(), TimeUnit.MILLISECONDS)
                        .exceptionally(throwable -> {
                            String code = getProviderCodeSafe(provider);
                            log.warn("Provider '{}' failed or timed out during search: {}", code, throwable.getMessage());
                            failedProviders.add(code);
                            return List.of();
                        }))
                .toList();

        // Wait for all provider executions to settle
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<NormalizedProductOffer> aggregatedOffers = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();

        // Transparently persist offers and price history snapshot
        if (!aggregatedOffers.isEmpty()) {
            catalogService.saveOffers(query, aggregatedOffers);
        }

        BestOption bestOption = calculateBestOption(aggregatedOffers);

        return new ProductSearchResponse(
                query,
                aggregatedOffers.size(),
                bestOption,
                aggregatedOffers,
                List.copyOf(failedProviders)
        );
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
