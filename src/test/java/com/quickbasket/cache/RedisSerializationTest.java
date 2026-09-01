package com.quickbasket.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbasket.dto.BestOption;
import com.quickbasket.dto.DeliveryEstimate;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import com.quickbasket.dto.ProductSearchResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("ProductSearchResponse record should correctly serialize and deserialize to/from JSON")
    void productSearchResponse_ShouldSerializeAndDeserializeCorrectly() throws Exception {
        NormalizedProductOffer offer = new NormalizedProductOffer(
                "BLINKIT",
                "Blinkit",
                PlatformType.QUICK_COMMERCE,
                new BigDecimal("54.00"),
                new BigDecimal("56.00"),
                new BigDecimal("3.57"),
                true,
                DeliveryEstimate.instant(14),
                "Blinkit Hub",
                "https://blinkit.com/item/123",
                "https://cdn.blinkit.com/img.jpg"
        );

        BestOption bestOption = new BestOption("BLINKIT", new BigDecimal("54.00"), "BLINKIT", 14);
        ProductSearchResponse original = new ProductSearchResponse("milk", 1, bestOption, List.of(offer), List.of());

        String json = objectMapper.writeValueAsString(original);
        assertThat(json).contains("\"platformCode\":\"BLINKIT\"");
        assertThat(json).contains("\"platformType\":\"QUICK_COMMERCE\"");

        ProductSearchResponse deserialized = objectMapper.readValue(json, ProductSearchResponse.class);
        assertThat(deserialized).isEqualTo(original);
        assertThat(deserialized.offers().get(0).platformType()).isEqualTo(PlatformType.QUICK_COMMERCE);
    }
}
