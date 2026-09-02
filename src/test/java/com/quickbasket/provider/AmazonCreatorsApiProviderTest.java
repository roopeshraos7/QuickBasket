package com.quickbasket.provider;

import com.quickbasket.dto.DeliveryType;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import com.quickbasket.exception.ProviderException;
import com.quickbasket.service.provider.AmazonCreatorsApiProvider;
import com.quickbasket.service.provider.AmazonTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AmazonCreatorsApiProviderTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;

    @Mock
    private AmazonTokenService tokenService;

    private AmazonCreatorsApiProvider provider;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        provider = new AmazonCreatorsApiProvider(
                restClientBuilder,
                tokenService,
                true,
                "test-client-id",
                "test-client-secret",
                "quickbasket-21",
                "www.amazon.in",
                "https://creatorsapi.amazon",
                1500L,
                10
        );
    }

    @Test
    @DisplayName("Provider metadata and supports should return correct Amazon identities")
    void providerMetadata_ShouldReturnCorrectIdentities() {
        assertThat(provider.getProviderCode()).isEqualTo("AMAZON");
        assertThat(provider.getPlatformType()).isEqualTo(PlatformType.ECOMMERCE);
        assertThat(provider.getTimeoutMs()).isEqualTo(1500L);
        assertThat(provider.isEnabled()).isTrue();

        assertThat(provider.supports("AMAZON")).isTrue();
        assertThat(provider.supports("amazon")).isTrue();
        assertThat(provider.supports("all")).isTrue();
        assertThat(provider.supports("flipkart")).isFalse();
    }

    @Test
    @DisplayName("searchProducts should return empty list when provider is disabled or credentials missing")
    void searchProducts_WhenDisabledOrMissingCredentials_ShouldReturnEmptyList() {
        AmazonCreatorsApiProvider disabledProvider = new AmazonCreatorsApiProvider(
                restClientBuilder, tokenService, false, "id", "secret", "tag", "www.amazon.in", "https://api.com", 1500L, 10
        );
        assertThat(disabledProvider.isEnabled()).isFalse();
        assertThat(disabledProvider.searchProducts("milk", "12.9716", "77.5946")).isEmpty();
    }

    @Test
    @DisplayName("searchProducts should execute POST /catalog/v1/searchItems with lowerCamelCase JSON and map offers")
    void searchProducts_SuccessfulResponse_ShouldMapNormalizedOffers() {
        when(tokenService.getAccessToken()).thenReturn("mock-bearer-token-999");

        String jsonResponse = """
                {
                  "searchResult": {
                    "totalResultCount": 1,
                    "items": [
                      {
                        "asin": "B0BX9N1D3L",
                        "detailPageURL": "https://www.amazon.in/dp/B0BX9N1D3L?tag=quickbasket-21",
                        "images": {
                          "primary": {
                            "medium": { "url": "https://m.media-amazon.com/images/I/71mob.jpg" }
                          }
                        },
                        "itemInfo": {
                          "title": { "displayValue": "Apple iPhone 15 (128 GB) - Black" }
                        },
                        "offersV2": {
                          "listings": [
                            {
                              "id": "L1",
                              "isBuyBoxWinner": true,
                              "price": {
                                "price": { "amount": 71999.00, "currency": "INR" },
                                "savingBasis": { "amount": 79900.00, "currency": "INR" },
                                "savingsPercentage": 9.89
                              },
                              "availability": { "type": "IN_STOCK", "message": "In stock" },
                              "merchantInfo": { "id": "A123", "name": "Appario Retail Private Ltd" }
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo("https://creatorsapi.amazon/catalog/v1/searchItems"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer mock-bearer-token-999"))
                .andExpect(header("x-marketplace", "www.amazon.in"))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(jsonPath("$.keywords").value("iphone"))
                .andExpect(jsonPath("$.partnerTag").value("quickbasket-21"))
                .andExpect(jsonPath("$.marketplace").value("www.amazon.in"))
                .andExpect(jsonPath("$.itemCount").value(10))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<NormalizedProductOffer> offers = provider.searchProducts("iphone", "12.9716", "77.5946");

        mockServer.verify();
        assertThat(offers).hasSize(1);
        NormalizedProductOffer offer = offers.get(0);

        assertThat(offer.platformCode()).isEqualTo("AMAZON");
        assertThat(offer.platformName()).isEqualTo("Amazon");
        assertThat(offer.platformType()).isEqualTo(PlatformType.ECOMMERCE);
        assertThat(offer.price()).isEqualByComparingTo(new BigDecimal("71999.00"));
        assertThat(offer.mrp()).isEqualByComparingTo(new BigDecimal("79900.00"));
        assertThat(offer.discountPercentage()).isEqualByComparingTo(new BigDecimal("9.89"));
        assertThat(offer.inStock()).isTrue();
        assertThat(offer.sellerName()).isEqualTo("Appario Retail Private Ltd");
        assertThat(offer.productUrl()).isEqualTo("https://www.amazon.in/dp/B0BX9N1D3L?tag=quickbasket-21");
        assertThat(offer.imageUrl()).isEqualTo("https://m.media-amazon.com/images/I/71mob.jpg");

        assertThat(offer.delivery()).isNotNull();
        assertThat(offer.delivery().type()).isEqualTo(DeliveryType.STANDARD);
        assertThat(offer.delivery().etaMinutes()).isNull();
        assertThat(offer.delivery().deliveryText()).isNull();
        assertThat(offer.delivery().shippingFee()).isNull();
    }

    @Test
    @DisplayName("searchProducts should map out of stock when availability type is OUTOFSTOCK")
    void searchProducts_OutOfStock_ShouldMapInStockFalse() {
        when(tokenService.getAccessToken()).thenReturn("mock-bearer-token-999");

        String jsonResponse = """
                {
                  "searchResult": {
                    "totalResultCount": 1,
                    "items": [
                      {
                        "asin": "B0123456",
                        "detailPageURL": "https://www.amazon.in/dp/B0123456",
                        "offersV2": {
                          "listings": [
                            {
                              "price": { "price": { "amount": 100.00, "currency": "INR" } },
                              "availability": { "type": "OUTOFSTOCK" }
                            }
                          ]
                        }
                      }
                    ]
                  }
                }
                """;

        mockServer.expect(requestTo("https://creatorsapi.amazon/catalog/v1/searchItems"))
                .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

        List<NormalizedProductOffer> offers = provider.searchProducts("item", "12.9716", "77.5946");
        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).inStock()).isFalse();
    }

    @Test
    @DisplayName("searchProducts on HTTP 400 Bad Request should throw ProviderException")
    void searchProducts_Http400_ShouldThrowProviderException() {
        when(tokenService.getAccessToken()).thenReturn("mock-bearer-token-999");

        mockServer.expect(requestTo("https://creatorsapi.amazon/catalog/v1/searchItems"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> provider.searchProducts("milk", "12.9716", "77.5946"))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("Amazon Creators API bad request");
    }

    @Test
    @DisplayName("searchProducts on HTTP 401/403 should throw ProviderException")
    void searchProducts_Http401_ShouldThrowProviderException() {
        when(tokenService.getAccessToken()).thenReturn("mock-bearer-token-999");

        mockServer.expect(requestTo("https://creatorsapi.amazon/catalog/v1/searchItems"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> provider.searchProducts("milk", "12.9716", "77.5946"))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("Amazon Creators API authentication failed");
    }

    @Test
    @DisplayName("searchProducts on HTTP 429 Rate Limit should throw ProviderException")
    void searchProducts_Http429_ShouldThrowProviderException() {
        when(tokenService.getAccessToken()).thenReturn("mock-bearer-token-999");

        mockServer.expect(requestTo("https://creatorsapi.amazon/catalog/v1/searchItems"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> provider.searchProducts("milk", "12.9716", "77.5946"))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("Amazon Creators API rate limit exceeded");
    }

    @Test
    @DisplayName("searchProducts on HTTP 500 Server Error should throw ProviderException")
    void searchProducts_Http500_ShouldThrowProviderException() {
        when(tokenService.getAccessToken()).thenReturn("mock-bearer-token-999");

        mockServer.expect(requestTo("https://creatorsapi.amazon/catalog/v1/searchItems"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> provider.searchProducts("milk", "12.9716", "77.5946"))
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("Amazon Creators API server error");
    }
}
