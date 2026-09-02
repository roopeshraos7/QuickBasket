package com.quickbasket.service.provider;

import com.quickbasket.dto.amazon.AmazonTokenResponse;
import com.quickbasket.exception.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * Dedicated OAuth 2.0 Client Credentials token management service for Amazon Creators API.
 * Manages thread-safe local in-memory token caching with a 5-minute expiry safety margin.
 */
@Service
public class AmazonTokenService {

    private static final Logger log = LoggerFactory.getLogger(AmazonTokenService.class);
    private static final long EXPIRY_SAFETY_MARGIN_SECONDS = 300L;

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String tokenUrl;

    private String cachedAccessToken;
    private Instant tokenExpiryTime = Instant.MIN;

    public AmazonTokenService(
            RestClient.Builder restClientBuilder,
            @Value("${quickbasket.providers.amazon.client-id:}") String clientId,
            @Value("${quickbasket.providers.amazon.client-secret:}") String clientSecret,
            @Value("${quickbasket.providers.amazon.token-url:https://api.amazon.co.in/auth/o2/token}") String tokenUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.clientId = clientId != null ? clientId.trim() : "";
        this.clientSecret = clientSecret != null ? clientSecret.trim() : "";
        this.tokenUrl = tokenUrl != null ? tokenUrl.trim() : "https://api.amazon.co.in/auth/o2/token";
    }

    /**
     * Retrieves a valid cached OAuth access token or refreshes it from LWA token endpoint.
     */
    public synchronized String getAccessToken() {
        if (cachedAccessToken != null && Instant.now().plusSeconds(EXPIRY_SAFETY_MARGIN_SECONDS).isBefore(tokenExpiryTime)) {
            log.debug("Reusing valid cached Amazon OAuth access token (expires at {})", tokenExpiryTime);
            return cachedAccessToken;
        }

        log.info("Requesting fresh Amazon OAuth access token from LWA endpoint '{}'", tokenUrl);

        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new ProviderException("Amazon client ID or client secret missing");
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("scope", "amazon_creators_api");

        try {
            AmazonTokenResponse response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, resp) -> {
                        throw new ProviderException("Amazon LWA token request failed: HTTP " + resp.getStatusCode());
                    })
                    .body(AmazonTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new ProviderException("Amazon LWA returned empty access token response");
            }

            long expiresInSeconds = response.expiresIn() != null ? response.expiresIn() : 3600L;
            this.tokenExpiryTime = Instant.now().plusSeconds(expiresInSeconds);
            this.cachedAccessToken = response.accessToken();

            log.info("Successfully acquired fresh Amazon OAuth access token (valid for {}s)", expiresInSeconds);
            return this.cachedAccessToken;

        } catch (ResourceAccessException e) {
            log.error("Network failure connecting to Amazon LWA token endpoint: {}", e.getMessage());
            throw new ProviderException("Amazon LWA token endpoint connection failure", e);
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error acquiring Amazon OAuth token: {}", e.getMessage());
            throw new ProviderException("Amazon LWA token acquisition error", e);
        }
    }

    /**
     * Clears cached token state (useful for test resets).
     */
    public synchronized void resetCache() {
        this.cachedAccessToken = null;
        this.tokenExpiryTime = Instant.MIN;
    }
}
