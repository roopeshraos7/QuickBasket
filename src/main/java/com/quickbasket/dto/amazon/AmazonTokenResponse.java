package com.quickbasket.dto.amazon;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth 2.0 Client Credentials token response payload from Login with Amazon (LWA).
 */
public record AmazonTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") Long expiresIn
) {}
