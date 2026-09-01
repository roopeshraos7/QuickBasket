package com.quickbasket.service;

import com.quickbasket.dto.DeliveryEstimate;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import com.quickbasket.dto.ProductSearchResponse;
import com.quickbasket.service.provider.ProductProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductComparisonServiceTest {

    @Mock
    private ProductProvider provider1;

    @Mock
    private ProductProvider provider2;

    @Mock
    private ProductCatalogService catalogService;

    private ProductComparisonService comparisonService;

    @BeforeEach
    void setUp() {
        lenient().when(provider1.supports(anyString())).thenReturn(true);
        lenient().when(provider1.isEnabled()).thenReturn(true);
        lenient().when(provider1.getProviderCode()).thenReturn("provider1");
        lenient().when(provider1.getTimeoutMs()).thenReturn(1500L);

        lenient().when(provider2.supports(anyString())).thenReturn(true);
        lenient().when(provider2.isEnabled()).thenReturn(true);
        lenient().when(provider2.getProviderCode()).thenReturn("provider2");
        lenient().when(provider2.getTimeoutMs()).thenReturn(1500L);

        comparisonService = new ProductComparisonService(List.of(provider1, provider2), catalogService, "all");
    }

    @Test
    @DisplayName("searchProducts should execute all active providers concurrently and calculate best option")
    void searchProducts_ShouldAggregateOffersAndCalculateBestOption() {
        List<NormalizedProductOffer> offers1 = List.of(
                new NormalizedProductOffer(
                        "BLINKIT", "Blinkit", PlatformType.QUICK_COMMERCE, new BigDecimal("54.00"), new BigDecimal("56.00"),
                        new BigDecimal("3.57"), true, DeliveryEstimate.instant(14), null, "http://link1", "http://img1"
                )
        );

        List<NormalizedProductOffer> offers2 = List.of(
                new NormalizedProductOffer(
                        "ZEPTO", "Zepto", PlatformType.QUICK_COMMERCE, new BigDecimal("50.00"), new BigDecimal("56.00"),
                        new BigDecimal("10.71"), true, DeliveryEstimate.instant(10), null, "http://link2", "http://img2"
                )
        );

        when(provider1.searchProducts(anyString(), anyString(), anyString())).thenReturn(offers1);
        when(provider2.searchProducts(anyString(), anyString(), anyString())).thenReturn(offers2);

        ProductSearchResponse response = comparisonService.searchProducts("Milk", "12.9716", "77.5946");

        assertThat(response).isNotNull();
        assertThat(response.query()).isEqualTo("Milk");
        assertThat(response.totalResults()).isEqualTo(2);

        // Cheapest should be Zepto at 50.00
        assertThat(response.bestOption().cheapestPlatformCode()).isEqualTo("ZEPTO");
        assertThat(response.bestOption().cheapestPrice()).isEqualTo(new BigDecimal("50.00"));

        // Fastest should be Zepto at 10 mins
        assertThat(response.bestOption().fastestPlatformCode()).isEqualTo("ZEPTO");
        assertThat(response.bestOption().fastestEtaMinutes()).isEqualTo(10);

        assertThat(response.failedProviders()).isEmpty();
    }

    @Test
    @DisplayName("searchProducts should handle provider failure gracefully and record failed provider code")
    void searchProducts_ShouldIsolateProviderFailureAndReturnPartialResults() {
        List<NormalizedProductOffer> offers1 = List.of(
                new NormalizedProductOffer(
                        "BLINKIT", "Blinkit", PlatformType.QUICK_COMMERCE, new BigDecimal("54.00"), new BigDecimal("56.00"),
                        new BigDecimal("3.57"), true, DeliveryEstimate.instant(14), null, "http://link1", "http://img1"
                )
        );

        when(provider1.searchProducts(anyString(), anyString(), anyString())).thenReturn(offers1);
        when(provider2.searchProducts(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("API connection timeout"));

        ProductSearchResponse response = comparisonService.searchProducts("Milk", "12.9716", "77.5946");

        assertThat(response).isNotNull();
        assertThat(response.totalResults()).isEqualTo(1);
        assertThat(response.offers().get(0).platformCode()).isEqualTo("BLINKIT");
        assertThat(response.failedProviders()).containsExactly("provider2");
    }
}
