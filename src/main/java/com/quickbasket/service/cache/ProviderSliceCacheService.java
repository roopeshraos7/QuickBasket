package com.quickbasket.service.cache;

import com.quickbasket.dto.NormalizedProductOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service orchestrating per-provider Redis slice caching.
 * Manages cache key generation, lookups, and cache put operations for individual provider result slices.
 */
@Service
public class ProviderSliceCacheService {

    private static final Logger log = LoggerFactory.getLogger(ProviderSliceCacheService.class);
    public static final String CACHE_NAME = "provider_slices";

    private final CacheManager cacheManager;

    @Autowired
    public ProviderSliceCacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Builds standard per-provider Redis cache key.
     * Format: qb:provider:<PROVIDER_CODE>:<NORMALIZED_QUERY>:<LATITUDE>:<LONGITUDE>
     */
    public String buildCacheKey(String providerCode, String query, String latitude, String longitude) {
        String code = (providerCode != null && !providerCode.isBlank()) ? providerCode.trim().toUpperCase() : "UNKNOWN";
        
        String normalizedQuery;
        if (query == null || query.isBlank()) {
            normalizedQuery = "empty";
        } else {
            normalizedQuery = query.trim().toLowerCase().replaceAll("\\s+", "+");
        }

        String lat = (latitude != null && !latitude.isBlank()) ? latitude.trim() : "default";
        String lng = (longitude != null && !longitude.isBlank()) ? longitude.trim() : "default";

        return String.format("qb:provider:%s:%s:%s:%s", code, normalizedQuery, lat, lng);
    }

    /**
     * Look up cached provider offer slice.
     */
    @SuppressWarnings("unchecked")
    public Optional<List<NormalizedProductOffer>> getSlice(String providerCode, String query, String latitude, String longitude) {
        String cacheKey = buildCacheKey(providerCode, query, latitude, longitude);
        try {
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(cacheKey);
                if (wrapper != null && wrapper.get() instanceof List<?>) {
                    log.debug("Cache HIT for provider '{}' key '{}'", providerCode, cacheKey);
                    return Optional.of((List<NormalizedProductOffer>) wrapper.get());
                }
            }
        } catch (Exception e) {
            log.warn("Redis GET failed for key '{}' on cache '{}': {}", cacheKey, CACHE_NAME, e.getMessage());
        }

        log.debug("Cache MISS for provider '{}' key '{}'", providerCode, cacheKey);
        return Optional.empty();
    }

    /**
     * Caches a fresh provider offer slice.
     */
    public void putSlice(String providerCode, String query, String latitude, String longitude, List<NormalizedProductOffer> offers) {
        if (offers == null) return;
        
        String cacheKey = buildCacheKey(providerCode, query, latitude, longitude);
        try {
            Cache cache = cacheManager.getCache(CACHE_NAME);
            if (cache != null) {
                cache.put(cacheKey, offers);
                log.debug("Cached provider slice for '{}' key '{}' ({} offers)", providerCode, cacheKey, offers.size());
            }
        } catch (Exception e) {
            log.warn("Redis PUT failed for key '{}' on cache '{}': {}", cacheKey, CACHE_NAME, e.getMessage());
        }
    }
}
