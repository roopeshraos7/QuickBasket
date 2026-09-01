package com.quickbasket.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class RedisCacheConfigTest {

    @Test
    @DisplayName("cacheManager should initialize RedisCacheManager bean correctly")
    void cacheManager_ShouldInitializeRedisCacheManager() {
        RedisCacheConfig redisCacheConfig = new RedisCacheConfig(300);
        RedisConnectionFactory mockFactory = mock(RedisConnectionFactory.class);

        RedisCacheManager cacheManager = redisCacheConfig.cacheManager(mockFactory);

        assertThat(cacheManager).isNotNull();
    }

    @Test
    @DisplayName("CacheErrorHandler should gracefully swallow Redis connection exceptions without throwing")
    void cacheErrorHandler_ShouldSwallowExceptionsGracefully() {
        RedisCacheConfig redisCacheConfig = new RedisCacheConfig(300);
        CacheErrorHandler errorHandler = redisCacheConfig.errorHandler();

        Cache mockCache = mock(Cache.class);
        RuntimeException redisException = new RuntimeException("Redis connection refused: localhost:6379");

        assertThatCode(() -> errorHandler.handleCacheGetError(redisException, mockCache, "qb:search:milk_12.97_77.59"))
                .doesNotThrowAnyException();

        assertThatCode(() -> errorHandler.handleCachePutError(redisException, mockCache, "qb:search:milk_12.97_77.59", "value"))
                .doesNotThrowAnyException();

        assertThatCode(() -> errorHandler.handleCacheEvictError(redisException, mockCache, "qb:search:milk_12.97_77.59"))
                .doesNotThrowAnyException();

        assertThatCode(() -> errorHandler.handleCacheClearError(redisException, mockCache))
                .doesNotThrowAnyException();
    }
}
