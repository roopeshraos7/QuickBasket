package com.quickbasket.cache;

import com.quickbasket.dto.BestOption;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.ProductSearchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisSerializationTest {

    @Test
    @DisplayName("GenericJackson2JsonRedisSerializer should correctly serialize and deserialize ProductSearchResponse Java record")
    void recordSerialization_ShouldSerializeAndDeserializeCorrectly() {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

        NormalizedProductOffer offer1 = new NormalizedProductOffer(
                "BLINKIT", "Blinkit", new BigDecimal("54.00"), new BigDecimal("56.00"),
                new BigDecimal("3.57"), true, 14, "http://link1", "http://img1"
        );
        NormalizedProductOffer offer2 = new NormalizedProductOffer(
                "ZEPTO", "Zepto", new BigDecimal("56.00"), new BigDecimal("56.00"),
                BigDecimal.ZERO, true, 10, "http://link2", "http://img2"
        );

        BestOption bestOption = new BestOption("BLINKIT", new BigDecimal("54.00"), "ZEPTO", 10);
        ProductSearchResponse originalResponse = new ProductSearchResponse("Milk", 2, bestOption, List.of(offer1, offer2));

        byte[] serializedBytes = serializer.serialize(originalResponse);
        assertThat(serializedBytes).isNotNull().isNotEmpty();

        Object deserializedObj = serializer.deserialize(serializedBytes);
        assertThat(deserializedObj).isInstanceOf(ProductSearchResponse.class);

        ProductSearchResponse deserializedResponse = (ProductSearchResponse) deserializedObj;
        assertThat(deserializedResponse.query()).isEqualTo("Milk");
        assertThat(deserializedResponse.totalResults()).isEqualTo(2);
        assertThat(deserializedResponse.bestOption().cheapestPlatformCode()).isEqualTo("BLINKIT");
        assertThat(deserializedResponse.bestOption().cheapestPrice()).isEqualTo(new BigDecimal("54.00"));
        assertThat(deserializedResponse.bestOption().fastestPlatformCode()).isEqualTo("ZEPTO");
        assertThat(deserializedResponse.bestOption().fastestEtaMinutes()).isEqualTo(10);
        assertThat(deserializedResponse.offers()).hasSize(2);
        assertThat(deserializedResponse.offers().get(0).platformCode()).isEqualTo("BLINKIT");
    }
}
