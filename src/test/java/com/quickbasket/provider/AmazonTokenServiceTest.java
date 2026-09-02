package com.quickbasket.provider;

import com.quickbasket.exception.ProviderException;
import com.quickbasket.service.provider.AmazonTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AmazonTokenServiceTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer mockServer;
    private AmazonTokenService tokenService;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        tokenService = new AmazonTokenService(
                restClientBuilder,
                "test-client-id",
                "test-client-secret",
                "https://api.amazon.co.uk/auth/o2/token"
        );
    }

    @Test
    @DisplayName("getAccessToken should request token via form POST with scope creatorsapi::default and cache valid access token")
    void getAccessToken_SuccessfulResponse_ShouldCacheAndReturnToken() {
        String tokenJsonResponse = """
                {
                  "access_token": "Atza|AmazonToken12345",
                  "token_type": "bearer",
                  "expires_in": 3600
                }
                """;

        mockServer.expect(requestTo("https://api.amazon.co.uk/auth/o2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("scope=creatorsapi%3A%3Adefault")))
                .andRespond(withSuccess(tokenJsonResponse, MediaType.APPLICATION_JSON));

        String token1 = tokenService.getAccessToken();
        assertThat(token1).isEqualTo("Atza|AmazonToken12345");

        // Second call should reuse cached token without making a second HTTP call
        String token2 = tokenService.getAccessToken();
        assertThat(token2).isEqualTo("Atza|AmazonToken12345");

        mockServer.verify();
    }

    @Test
    @DisplayName("getAccessToken on HTTP 400/401 token endpoint failure should throw ProviderException")
    void getAccessToken_HttpError_ShouldThrowProviderException() {
        mockServer.expect(requestTo("https://api.amazon.co.uk/auth/o2/token"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> tokenService.getAccessToken())
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("Amazon LWA token request failed");
    }

    @Test
    @DisplayName("getAccessToken with missing credentials should throw ProviderException without HTTP call")
    void getAccessToken_MissingCredentials_ShouldThrowProviderException() {
        AmazonTokenService emptyCredsService = new AmazonTokenService(
                restClientBuilder, "", "", "https://api.amazon.co.uk/auth/o2/token"
        );

        assertThatThrownBy(() -> emptyCredsService.getAccessToken())
                .isInstanceOf(ProviderException.class)
                .hasMessageContaining("Amazon client ID or client secret missing");
    }
}
