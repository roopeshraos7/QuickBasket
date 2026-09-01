package com.quickbasket.service;

import com.quickbasket.dto.BestOption;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.ProductSearchResponse;
import com.quickbasket.service.provider.MockProductProvider;
import com.quickbasket.service.provider.ProductProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Service encapsulating product search, provider resolution, and price/ETA comparison calculations.
 */
@Service
public class ProductComparisonService {

    private static final Logger log = LoggerFactory.getLogger(ProductComparisonService.class);

    private final List<ProductProvider> providers;
    private final ProductCatalogService catalogService;
    private final String activeProviderCode;

    public ProductComparisonService(
            List<ProductProvider> providers,
            ProductCatalogService catalogService,
            @Value("${quickcommerce.api.active-provider:mock}") String activeProviderCode
    ) {
        this.providers = providers;
        this.catalogService = catalogService;
        this.activeProviderCode = activeProviderCode;
    }

    /**
     * Search products across platforms using the configured active provider,
     * persist/update offers and price history, and compute best options.
     * Caches search result in Redis under 'product_searches' for 5 minutes.
     *
     * @param query     Product search query
     * @param latitude  User location latitude
     * @param longitude User location longitude
     * @return ProductSearchResponse containing offers and best option analysis
     */
    @Cacheable(value = "product_searches", key = "'qb:search:' + (#query != null ? #query.toLowerCase().trim() : '') + '_' + (#latitude != null ? #latitude : 'default') + '_' + (#longitude != null ? #longitude : 'default')")
    public ProductSearchResponse searchProducts(String query, String latitude, String longitude) {
        ProductProvider provider = resolveProvider(activeProviderCode);
        log.info("Executing product search using provider '{}' for query '{}'", activeProviderCode, query);

        List<NormalizedProductOffer> offers = provider.searchProducts(query, latitude, longitude);
        
        // Transparently persist offers and price history snapshot
        catalogService.saveOffers(query, offers);

        BestOption bestOption = calculateBestOption(offers);

        return new ProductSearchResponse(
                query,
                offers.size(),
                bestOption,
                offers
        );
    }

    private ProductProvider resolveProvider(String providerCode) {
        return providers.stream()
                .filter(p -> p.supports(providerCode))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Requested provider '{}' not found. Falling back to MockProductProvider", providerCode);
                    return providers.stream()
                            .filter(p -> p.supports(MockProductProvider.PROVIDER_CODE))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("No valid ProductProvider implementation registered."));
                });
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
                .min(Comparator.comparing(NormalizedProductOffer::etaMinutes))
                .orElse(inStockOffers.get(0));

        return new BestOption(
                cheapest.platformCode(),
                cheapest.price(),
                fastest.platformCode(),
                fastest.etaMinutes()
        );
    }
}
