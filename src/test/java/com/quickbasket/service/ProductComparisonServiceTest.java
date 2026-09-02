package com.quickbasket.service;

import com.quickbasket.dto.DeliveryEstimate;
import com.quickbasket.dto.DeliveryType;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import com.quickbasket.dto.ProductSearchResponse;
import com.quickbasket.service.cache.ProviderSliceCacheService;
import com.quickbasket.service.provider.ProductProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductComparisonServiceTest {

    @Mock
    private ProductProvider provider1;

    @Mock
    private ProductProvider provider2;

    @Mock
    private ProductProvider flipkartProvider;

    @Mock
    private ProductCatalogService catalogService;

    @Mock
    private ProviderSliceCacheService sliceCacheService;

    private ProductComparisonService comparisonService;

    @BeforeEach
    void setUp() {
        lenient().when(provider1.supports(anyString())).thenReturn(true);
        lenient().when(provider1.isEnabled()).thenReturn(true);
        lenient().when(provider1.getProviderCode()).thenReturn("MOCK");
        lenient().when(provider1.getTimeoutMs()).thenReturn(1500L);

        lenient().when(provider2.supports(anyString())).thenReturn(true);
        lenient().when(provider2.isEnabled()).thenReturn(true);
        lenient().when(provider2.getProviderCode()).thenReturn("QUICKCOMMERCE_API");
        lenient().when(provider2.getTimeoutMs()).thenReturn(1500L);

        lenient().when(flipkartProvider.supports(anyString())).thenReturn(true);
        lenient().when(flipkartProvider.isEnabled()).thenReturn(true);
        lenient().when(flipkartProvider.getProviderCode()).thenReturn("FLIPKART");
        lenient().when(flipkartProvider.getPlatformType()).thenReturn(PlatformType.ECOMMERCE);
        lenient().when(flipkartProvider.getTimeoutMs()).thenReturn(2000L);

        comparisonService = new ProductComparisonService(
                List.of(provider1, provider2, flipkartProvider),
                catalogService,
                sliceCacheService,
                "all"
        );
    }

    @Test
    @DisplayName("searchProducts should execute provider on Cache MISS, cache slice, persist to DB, and calculate best option")
    void searchProducts_CacheMiss_ShouldExecuteProviderAndPersist() {
        NormalizedProductOffer offer1 = new NormalizedProductOffer(
                "BLINKIT", "Blinkit", PlatformType.QUICK_COMMERCE, new BigDecimal("54.00"), new BigDecimal("56.00"),
                new BigDecimal("3.57"), true, DeliveryEstimate.instant(14), null, "http://link1", "http://img1"
        );

        NormalizedProductOffer offer2 = new NormalizedProductOffer(
                "ZEPTO", "Zepto", PlatformType.QUICK_COMMERCE, new BigDecimal("50.00"), new BigDecimal("56.00"),
                new BigDecimal("10.71"), true, DeliveryEstimate.instant(10), null, "http://link2", "http://img2"
        );

        when(sliceCacheService.getSlice(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(provider1.searchProducts(anyString(), anyString(), anyString())).thenReturn(List.of(offer1));
        when(provider2.searchProducts(anyString(), anyString(), anyString())).thenReturn(List.of(offer2));
        when(flipkartProvider.searchProducts(anyString(), anyString(), anyString())).thenReturn(List.of());

        ProductSearchResponse response = comparisonService.searchProducts("Milk", "12.9716", "77.5946");

        assertThat(response).isNotNull();
        assertThat(response.query()).isEqualTo("Milk");
        assertThat(response.totalResults()).isEqualTo(2);

        // Best option calculation
        assertThat(response.bestOption().cheapestPlatformCode()).isEqualTo("ZEPTO");
        assertThat(response.bestOption().cheapestPrice()).isEqualTo(new BigDecimal("50.00"));
        assertThat(response.bestOption().fastestPlatformCode()).isEqualTo("ZEPTO");
        assertThat(response.bestOption().fastestEtaMinutes()).isEqualTo(10);

        // Verify cache puts for all providers
        verify(sliceCacheService).putSlice("MOCK", "Milk", "12.9716", "77.5946", List.of(offer1));
        verify(sliceCacheService).putSlice("QUICKCOMMERCE_API", "Milk", "12.9716", "77.5946", List.of(offer2));
        verify(sliceCacheService).putSlice("FLIPKART", "Milk", "12.9716", "77.5946", List.of());

        // Verify DB persistence occurred for fresh offers
        verify(catalogService).saveOffers(eq("Milk"), anyList());
    }

    @Test
    @DisplayName("searchProducts should return cached slice on Cache HIT without invoking provider or persisting to DB")
    void searchProducts_CacheHit_ShouldReturnCachedSliceWithoutProviderInvocationOrDbSave() {
        NormalizedProductOffer offer1 = new NormalizedProductOffer(
                "BLINKIT", "Blinkit", PlatformType.QUICK_COMMERCE, new BigDecimal("54.00"), new BigDecimal("56.00"),
                new BigDecimal("3.57"), true, DeliveryEstimate.instant(14), null, "http://link1", "http://img1"
        );

        when(sliceCacheService.getSlice("MOCK", "Milk", "12.9716", "77.5946")).thenReturn(Optional.of(List.of(offer1)));
        when(sliceCacheService.getSlice("QUICKCOMMERCE_API", "Milk", "12.9716", "77.5946")).thenReturn(Optional.of(List.of()));
        when(sliceCacheService.getSlice("FLIPKART", "Milk", "12.9716", "77.5946")).thenReturn(Optional.of(List.of()));

        ProductSearchResponse response = comparisonService.searchProducts("Milk", "12.9716", "77.5946");

        assertThat(response).isNotNull();
        assertThat(response.totalResults()).isEqualTo(1);
        assertThat(response.offers().get(0).platformCode()).isEqualTo("BLINKIT");

        // Verify providers were NOT invoked
        verify(provider1, never()).searchProducts(anyString(), anyString(), anyString());
        verify(provider2, never()).searchProducts(anyString(), anyString(), anyString());
        verify(flipkartProvider, never()).searchProducts(anyString(), anyString(), anyString());

        // Verify DB persistence was NOT invoked on cache hit
        verify(catalogService, never()).saveOffers(anyString(), anyList());
    }

    @Test
    @DisplayName("searchProducts should handle Mixed HIT and MISS cleanly across QuickCommerce and Flipkart providers")
    void searchProducts_MixedHitAndMiss() {
        NormalizedProductOffer offerCached = new NormalizedProductOffer(
                "BLINKIT", "Blinkit", PlatformType.QUICK_COMMERCE, new BigDecimal("54.00"), new BigDecimal("56.00"),
                new BigDecimal("3.57"), true, DeliveryEstimate.instant(14), null, "http://link1", "http://img1"
        );

        NormalizedProductOffer offerFlipkart = new NormalizedProductOffer(
                "FLIPKART", "Flipkart", PlatformType.ECOMMERCE, new BigDecimal("45.00"), new BigDecimal("50.00"),
                new BigDecimal("10.00"), true, new DeliveryEstimate(DeliveryType.STANDARD, null, "In 2 days", BigDecimal.ZERO),
                "Appario", "http://fk.com/p", "http://fk.com/img"
        );

        // Provider 1 (MOCK): HIT
        when(sliceCacheService.getSlice("MOCK", "Milk", "12.9716", "77.5946")).thenReturn(Optional.of(List.of(offerCached)));
        // Provider 2 (QUICKCOMMERCE_API): HIT (empty)
        when(sliceCacheService.getSlice("QUICKCOMMERCE_API", "Milk", "12.9716", "77.5946")).thenReturn(Optional.of(List.of()));
        // FlipkartProvider: MISS
        when(sliceCacheService.getSlice("FLIPKART", "Milk", "12.9716", "77.5946")).thenReturn(Optional.empty());
        when(flipkartProvider.searchProducts("Milk", "12.9716", "77.5946")).thenReturn(List.of(offerFlipkart));

        ProductSearchResponse response = comparisonService.searchProducts("Milk", "12.9716", "77.5946");

        assertThat(response).isNotNull();
        assertThat(response.totalResults()).isEqualTo(2);

        // Flipkart should be cheapest at 45.00
        assertThat(response.bestOption().cheapestPlatformCode()).isEqualTo("FLIPKART");
        assertThat(response.bestOption().cheapestPrice()).isEqualTo(new BigDecimal("45.00"));

        // Blinkit should be fastest at 14 mins (Flipkart has null etaMinutes)
        assertThat(response.bestOption().fastestPlatformCode()).isEqualTo("BLINKIT");
        assertThat(response.bestOption().fastestEtaMinutes()).isEqualTo(14);

        // Only Flipkart should be invoked
        verify(provider1, never()).searchProducts(anyString(), anyString(), anyString());
        verify(provider2, never()).searchProducts(anyString(), anyString(), anyString());
        verify(flipkartProvider).searchProducts("Milk", "12.9716", "77.5946");

        // DB should be saved ONLY for fresh Flipkart offers
        verify(catalogService).saveOffers("Milk", List.of(offerFlipkart));
    }

    @Test
    @DisplayName("searchProducts should isolate Flipkart provider failure and record failed provider code without caching failure")
    void searchProducts_ShouldIsolateProviderFailureAndReturnPartialResults() {
        NormalizedProductOffer offer1 = new NormalizedProductOffer(
                "BLINKIT", "Blinkit", PlatformType.QUICK_COMMERCE, new BigDecimal("54.00"), new BigDecimal("56.00"),
                new BigDecimal("3.57"), true, DeliveryEstimate.instant(14), null, "http://link1", "http://img1"
        );

        when(sliceCacheService.getSlice(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(provider1.searchProducts(anyString(), anyString(), anyString())).thenReturn(List.of(offer1));
        when(provider2.searchProducts(anyString(), anyString(), anyString())).thenReturn(List.of());
        when(flipkartProvider.searchProducts(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Flipkart API rate limit exceeded"));

        ProductSearchResponse response = comparisonService.searchProducts("Milk", "12.9716", "77.5946");

        assertThat(response).isNotNull();
        assertThat(response.totalResults()).isEqualTo(1);
        assertThat(response.offers().get(0).platformCode()).isEqualTo("BLINKIT");
        assertThat(response.failedProviders()).containsExactly("FLIPKART");

        // Verify Flipkart failure slice WAS NOT cached
        verify(sliceCacheService, never()).putSlice(eq("FLIPKART"), anyString(), anyString(), anyString(), anyList());
    }
}
