package com.quickbasket.cache;

import com.quickbasket.dto.DeliveryEstimate;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import com.quickbasket.service.cache.ProviderSliceCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderSliceCacheServiceTest {

    private CacheManager cacheManager;
    private ProviderSliceCacheService sliceCacheService;

    @BeforeEach
    void setUp() {
        cacheManager = new ConcurrentMapCacheManager(ProviderSliceCacheService.CACHE_NAME);
        sliceCacheService = new ProviderSliceCacheService(cacheManager);
    }

    @Test
    @DisplayName("buildCacheKey should normalize provider code, query spaces, case, and coordinates")
    void buildCacheKey_ShouldNormalizeQueryAndCoordinates() {
        String key1 = sliceCacheService.buildCacheKey("mock", "  AMUL   Milk  ", " 12.9716 ", " 77.5946 ");
        assertThat(key1).isEqualTo("qb:provider:MOCK:amul+milk:12.9716:77.5946");

        String key2 = sliceCacheService.buildCacheKey("quickcommerce_api", null, null, "");
        assertThat(key2).isEqualTo("qb:provider:QUICKCOMMERCE_API:empty:default:default");
    }

    @Test
    @DisplayName("getSlice should return empty Optional on Cache MISS and populated Optional on Cache HIT")
    void getSlice_CacheMissAndHit() {
        String providerCode = "MOCK";
        String query = "bread";
        String lat = "12.9716";
        String lng = "77.5946";

        // MISS
        Optional<List<NormalizedProductOffer>> miss = sliceCacheService.getSlice(providerCode, query, lat, lng);
        assertThat(miss).isEmpty();

        // PUT
        NormalizedProductOffer offer = new NormalizedProductOffer(
                "BLINKIT", "Blinkit", PlatformType.QUICK_COMMERCE,
                new BigDecimal("38.00"), new BigDecimal("40.00"), new BigDecimal("5.00"),
                true, DeliveryEstimate.instant(12), "Hub", "http://url", "http://img"
        );
        sliceCacheService.putSlice(providerCode, query, lat, lng, List.of(offer));

        // HIT
        Optional<List<NormalizedProductOffer>> hit = sliceCacheService.getSlice(providerCode, query, lat, lng);
        assertThat(hit).isPresent();
        assertThat(hit.get()).hasSize(1);
        assertThat(hit.get().get(0).platformCode()).isEqualTo("BLINKIT");
    }
}
