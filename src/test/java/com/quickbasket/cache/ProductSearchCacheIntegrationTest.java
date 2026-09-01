package com.quickbasket.cache;

import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.ProductSearchResponse;
import com.quickbasket.service.ProductCatalogService;
import com.quickbasket.service.ProductComparisonService;
import com.quickbasket.service.provider.MockProductProvider;
import com.quickbasket.service.provider.ProductProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
class ProductSearchCacheIntegrationTest {

    @Autowired
    private ProductComparisonService comparisonService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private ProductCatalogService catalogService;

    @MockBean
    private MockProductProvider mockProductProvider;

    @BeforeEach
    void setUp() {
        when(mockProductProvider.supports(anyString())).thenReturn(true);
        if (cacheManager.getCache("product_searches") != null) {
            cacheManager.getCache("product_searches").clear();
        }
    }

    @Test
    @DisplayName("Cache MISS should execute provider; Cache HIT should return cached response and skip provider execution")
    void searchProducts_CacheMissExecutesProvider_CacheHitSkipsProvider() {
        List<NormalizedProductOffer> mockOffers = List.of(
                new NormalizedProductOffer(
                        "BLINKIT", "Blinkit", new BigDecimal("54.00"), new BigDecimal("56.00"),
                        new BigDecimal("3.57"), true, 14, "http://link1", "http://img1"
                )
        );

        when(mockProductProvider.searchProducts(anyString(), anyString(), anyString())).thenReturn(mockOffers);

        // --- Call 1: Cache MISS ---
        ProductSearchResponse response1 = comparisonService.searchProducts("Milk", "12.9716", "77.5946");
        assertThat(response1).isNotNull();
        assertThat(response1.query()).isEqualTo("Milk");

        // Verify provider and catalog service were called 1 time
        verify(mockProductProvider, times(1)).searchProducts("Milk", "12.9716", "77.5946");
        verify(catalogService, times(1)).saveOffers("Milk", mockOffers);

        // --- Call 2: Identical request -> Cache HIT ---
        ProductSearchResponse response2 = comparisonService.searchProducts("Milk", "12.9716", "77.5946");
        assertThat(response2).isNotNull();
        assertThat(response2.query()).isEqualTo("Milk");

        // Provider and catalog service must NOT be called a second time (invocation count remains 1)
        verify(mockProductProvider, times(1)).searchProducts("Milk", "12.9716", "77.5946");
        verify(catalogService, times(1)).saveOffers("Milk", mockOffers);

        // --- Call 3: Different Query -> Cache MISS ---
        ProductSearchResponse response3 = comparisonService.searchProducts("Bread", "12.9716", "77.5946");
        assertThat(response3).isNotNull();

        // Provider must be called for the new key (total invocations = 2)
        verify(mockProductProvider, times(1)).searchProducts("Bread", "12.9716", "77.5946");
    }
}
