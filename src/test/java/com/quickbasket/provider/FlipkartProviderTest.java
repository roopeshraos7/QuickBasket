package com.quickbasket.provider;

import com.quickbasket.dto.DeliveryType;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import com.quickbasket.exception.ProviderException;
import com.quickbasket.service.provider.FlipkartProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FlipkartProviderTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private FlipkartProvider provider;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        provider = new FlipkartProvider(
                restClientBuilder,
                true,
                "test-aff-id",
                "test-aff-token",
                "https://affiliate-api.flipkart.net",
                2000L,
                10
        );
    }

    @Test
    @DisplayName("Provider metadata and supports should return correct Flipkart identities")
    void providerMetadata_ShouldReturnCorrectIdentities() {
        assertThat(provider.getProviderCode()).isEqualTo("FLIPKART");
        assertThat(provider.getPlatformType()).isEqualTo(PlatformType.ECOMMERCE);
        assertThat(provider.getTimeoutMs()).isEqualTo(2000L);
        assertThat(provider.isEnabled()).isTrue();

        assertThat(provider.supports("FLIPKART")).isTrue();
        assertThat(provider.supports("flipkart")).isTrue();
        assertThat(provider.supports("all")).isTrue();
        assertThat(provider.supports("mock")).isFalse();
    }

    @Test
    @DisplayName("searchProducts should return empty list when provider is disabled or credentials missing")
    void searchProducts_WhenDisabledOrMissingCredentials_ShouldReturnEmptyList() {
        FlipkartProvider disabledProvider = new FlipkartProvider(
                restClientBuilder, false, "id", "token", "https://api.com", 2000L, 10
        );
        assertThat(disabledProvider.isEnabled()).isFalse();
        assertThat(disabledProvider.searchProducts("milk", "12.9716", "77.5946")).isEmpty();

        FlipkartProvider noCredsProvider = new FlipkartProvider(
                restClientBuilder, true, "", "", "https://api.com", 2000L, 10
        );
        assertThat(noCredsProvider.isEnabled()).isFalse();
        assertThat(noCredsProvider.searchProducts("milk", "12.9716", "77.5946")).isEmpty();
    }

    @Test
    @DisplayName("searchProducts should construct valid headers, params to /affiliate/1.0/search.json and map v1 response payload")
    void searchProducts_SuccessfulV1Response_ShouldMapNormalizedOffers() {
        String jsonResponse = """
                {
                  "productInfoList": [
                    {
                      "productBaseInfoV1": {
                        "productIdentifier": {
                          "productId": "MOBFKYZ3HZXFGH2G"
                        },
                        "productAttributes": {
                          "title": "Samsung Galaxy S23 5G",
                          "productBrand": "Samsung",
                          "sellingPrice": { "amount": 64999.00, "currency": "INR" },
                          "maximumRetailPrice": { "amount": 74999.00, "currency": "INR" },
                          "discountPercentage": 13.33,
                          "inStock": true,
                          "isAvailable": true,
                          "productUrl": "https://dl.flipkart.com/dl/samsung?affid=test-aff-id",
                          "imageUrls": { "400x400": "https://img.fkcdn.com/mob.jpg" },
                          "sellerName": "Appario Retail",
                          "shippingInfo": {
                            "shippingFees": { "amount": 0.00, "currency": "INR" },
                            "estimatedDelivery": "Delivery in 2-3 business days"
                          }
                        }
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://affiliate-api.flipkart.net/affiliate/1.0/search.json?query=samsung&resultCount=10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Fk-Affiliate-Id", "test-aff-id"))
                .andExpect(header("Fk-Affiliate-Token", "test-aff-token"))
                .andExpect(header("Accept", "application/json"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<NormalizedProductOffer> offers = provider.searchProducts("samsung", "12.9716", "77.5946");

        mockServer.verify();
        assertThat(offers).hasSize(1);
        NormalizedProductOffer offer = offers.get(0);

        assertThat(offer.platformCode()).isEqualTo("FLIPKART");
        assertThat(offer.platformName()).isEqualTo("Flipkart");
        assertThat(offer.platformType()).isEqualTo(PlatformType.ECOMMERCE);
        assertThat(offer.price()).isEqualByComparingTo(new BigDecimal("64999.00"));
        assertThat(offer.mrp()).isEqualByComparingTo(new BigDecimal("74999.00"));
        assertThat(offer.discountPercentage()).isEqualByComparingTo(new BigDecimal("13.33"));
        assertThat(offer.inStock()).isTrue();
        assertThat(offer.sellerName()).isEqualTo("Appario Retail");
        assertThat(offer.productUrl()).isEqualTo("https://dl.flipkart.com/dl/samsung?affid=test-aff-id");
        assertThat(offer.imageUrl()).isEqualTo("https://img.fkcdn.com/mob.jpg");

        assertThat(offer.delivery()).isNotNull();
        assertThat(offer.delivery().type()).isEqualTo(DeliveryType.STANDARD);
        assertThat(offer.delivery().etaMinutes()).isNull();
        assertThat(offer.delivery().deliveryText()).isEqualTo("Delivery in 2-3 business days");
        assertThat(offer.delivery().shippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("searchProducts should handle null/missing optional fields safely")
    void searchProducts_MissingOptionalFields_ShouldMapSafelyWithDefaults() {
        String jsonResponse = """
                {
                  "productInfoList": [
                    {
                      "productBaseInfoV1": {
                        "productIdentifier": { "productId": "MINIMAL123" },
                        "productAttributes": {
                          "title": "Minimal Item",
                          "sellingPrice": { "amount": 250.00, "currency": "INR" },
                          "maximumRetailPrice": { "amount": 300.00, "currency": "INR" },
                          "productUrl": "http://fk.com/minimal"
                        }
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://affiliate-api.flipkart.net/affiliate/1.0/search.json?query=minimal&resultCount=10"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<NormalizedProductOffer> offers = provider.searchProducts("minimal", "12.9716", "77.5946");

        assertThat(offers).hasSize(1);
        NormalizedProductOffer offer = offers.get(0);

        assertThat(offer.price()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(offer.mrp()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(offer.discountPercentage()).isEqualByComparingTo(new BigDecimal("16.67")); // Calculated fallback
        assertThat(offer.inStock()).isTrue(); // Defaults to true when null
        assertThat(offer.sellerName()).isEqualTo("Flipkart Seller");
        assertThat(offer.imageUrl()).isNull();
        assertThat(offer.delivery().deliveryText()).isEqualTo("Standard delivery in 2-4 business days");
        assertThat(offer.delivery().shippingFee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("searchProducts should return empty list when productInfoList is empty")
    void searchProducts_EmptyProductInfoList_ShouldReturnEmptyList() {
        String jsonResponse = """
                {
                  "productInfoList": []
                }
                """;

        mockServer.expect(requestTo("https://affiliate-api.flipkart.net/affiliate/1.0/search.json?query=unknown&resultCount=10"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<NormalizedProductOffer> offers = provider.searchProducts("unknown", "12.9716", "77.5946");
        assertThat(offers).isEmpty();
    }

    @Test
    @DisplayName("searchProducts on HTTP 401/403 should throw ProviderException")
    void searchProducts_Http401_ShouldThrowProviderException() {
        mockServer.expect(requestTo("https://affiliate-api.flipkart.net/affiliate/1.0/search.json?query=milk&resultCount=10"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> provider.searchProducts("milk", "12.9716", "77.5946"))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("Flipkart API authentication failed");
    }

    @Test
    @DisplayName("searchProducts on HTTP 429 Rate Limit should throw ProviderException")
    void searchProducts_Http429_ShouldThrowProviderException() {
        mockServer.expect(requestTo("https://affiliate-api.flipkart.net/affiliate/1.0/search.json?query=milk&resultCount=10"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.searchProducts("milk", "12.9716", "77.5946"))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("Flipkart API rate limit exceeded");
    }

    @Test
    @DisplayName("searchProducts on HTTP 5xx Server Error should throw ProviderException")
    void searchProducts_Http500_ShouldThrowProviderException() {
        mockServer.expect(requestTo("https://affiliate-api.flipkart.net/affiliate/1.0/search.json?query=milk&resultCount=10"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> provider.searchProducts("milk", "12.9716", "77.5946"))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("Flipkart API server error");
    }
}
