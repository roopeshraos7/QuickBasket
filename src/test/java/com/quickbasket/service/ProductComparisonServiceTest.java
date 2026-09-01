package com.quickbasket.service;

import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.ProductSearchResponse;
import com.quickbasket.service.provider.MockProductProvider;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductComparisonServiceTest {

    @Mock
    private ProductProvider mockProvider;

    @Mock
    private ProductCatalogService catalogService;

    private ProductComparisonService comparisonService;

    @BeforeEach
    void setUp() {
        when(mockProvider.supports(MockProductProvider.PROVIDER_CODE)).thenReturn(true);
        comparisonService = new ProductComparisonService(List.of(mockProvider), catalogService, "mock");
    }

    @Test
    @DisplayName("searchProducts should calculate cheapest price and fastest ETA correctly")
    void searchProducts_ShouldCalculateCheapestAndFastestOptions() {
        List<NormalizedProductOffer> mockOffers = List.of(
                new NormalizedProductOffer(
                        "BLINKIT", "Blinkit", new BigDecimal("54.00"), new BigDecimal("56.00"),
                        new BigDecimal("3.57"), true, 14, "http://link1", "http://img1"
                ),
                new NormalizedProductOffer(
                        "ZEPTO", "Zepto", new BigDecimal("56.00"), new BigDecimal("56.00"),
                        BigDecimal.ZERO, true, 10, "http://link2", "http://img2"
                ),
                new NormalizedProductOffer(
                        "INSTAMART", "Instamart", new BigDecimal("52.00"), new BigDecimal("56.00"),
                        new BigDecimal("7.14"), true, 20, "http://link3", "http://img3"
                )
        );

        when(mockProvider.searchProducts(anyString(), anyString(), anyString())).thenReturn(mockOffers);

        ProductSearchResponse response = comparisonService.searchProducts("Milk", "12.9716", "77.5946");

        assertThat(response).isNotNull();
        assertThat(response.query()).isEqualTo("Milk");
        assertThat(response.totalResults()).isEqualTo(3);

        // Cheapest should be Instamart at 52.00
        assertThat(response.bestOption().cheapestPlatformCode()).isEqualTo("INSTAMART");
        assertThat(response.bestOption().cheapestPrice()).isEqualTo(new BigDecimal("52.00"));

        // Fastest should be Zepto at 10 mins
        assertThat(response.bestOption().fastestPlatformCode()).isEqualTo("ZEPTO");
        assertThat(response.bestOption().fastestEtaMinutes()).isEqualTo(10);
    }
}
