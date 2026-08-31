package com.quickbasket.provider;

import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.service.provider.MockProductProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockProductProviderTest {

    private MockProductProvider mockProvider;

    @BeforeEach
    void setUp() {
        mockProvider = new MockProductProvider();
    }

    @Test
    @DisplayName("supports should return true for 'mock' provider code")
    void supports_ShouldReturnTrueForMockCode() {
        assertThat(mockProvider.supports("mock")).isTrue();
        assertThat(mockProvider.supports("MOCK")).isTrue();
        assertThat(mockProvider.supports("quickcommerce")).isFalse();
    }

    @Test
    @DisplayName("searchProducts should return deterministic milk offers for general query")
    void searchProducts_ShouldReturnMilkOffersForGeneralQuery() {
        List<NormalizedProductOffer> offers = mockProvider.searchProducts("Amul Milk", "12.9716", "77.5946");

        assertThat(offers).hasSize(3);
        assertThat(offers.stream().map(NormalizedProductOffer::platformCode))
                .containsExactlyInAnyOrder("BLINKIT", "ZEPTO", "INSTAMART");

        NormalizedProductOffer blinkitOffer = offers.stream()
                .filter(o -> "BLINKIT".equals(o.platformCode()))
                .findFirst()
                .orElseThrow();

        assertThat(blinkitOffer.price()).isEqualTo(new BigDecimal("54.00"));
        assertThat(blinkitOffer.mrp()).isEqualTo(new BigDecimal("56.00"));
        assertThat(blinkitOffer.discountPercentage()).isEqualTo(new BigDecimal("3.57"));
        assertThat(blinkitOffer.inStock()).isTrue();
    }

    @Test
    @DisplayName("searchProducts should return bread offers when query contains bread")
    void searchProducts_ShouldReturnBreadOffersForBreadQuery() {
        List<NormalizedProductOffer> offers = mockProvider.searchProducts("brown bread", "12.9716", "77.5946");

        assertThat(offers).hasSize(2);
        assertThat(offers.stream().map(NormalizedProductOffer::platformCode))
                .containsExactlyInAnyOrder("BLINKIT", "ZEPTO");
    }
}
