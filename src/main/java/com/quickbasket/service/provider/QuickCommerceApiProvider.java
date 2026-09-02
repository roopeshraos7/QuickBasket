package com.quickbasket.service.provider;

import com.quickbasket.dto.DeliveryEstimate;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import com.quickbasket.dto.QuickCommerceRawOffer;
import com.quickbasket.dto.QuickCommerceRawResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Third-party QuickCommerceAPI.com provider integration.
 * Represents an external aggregator integration source (QUICKCOMMERCE_API).
 */
@Component
public class QuickCommerceApiProvider implements ProductProvider {

    private static final Logger log = LoggerFactory.getLogger(QuickCommerceApiProvider.class);

    private final RestClient restClient;
    private final String apiToken;
    private final boolean enabled;
    private final long timeoutMs;

    public QuickCommerceApiProvider(
            RestClient.Builder restClientBuilder,
            @Value("${quickcommerce.api.base-url:https://api.quickcommerceapi.com}") String baseUrl,
            @Value("${quickcommerce.api.token:mock-token}") String apiToken,
            @Value("${quickbasket.providers.quickcommerce-api.enabled:false}") boolean enabled,
            @Value("${quickbasket.providers.quickcommerce-api.timeout-ms:1500}") long timeoutMs
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiToken = apiToken;
        this.enabled = enabled;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public List<NormalizedProductOffer> searchProducts(String query, String latitude, String longitude) {
        log.info("Fetching quick-commerce offers for query '{}' (lat: {}, lng: {})", query, latitude, longitude);

        try {
            QuickCommerceRawResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search")
                            .queryParam("query", query)
                            .queryParam("lat", latitude)
                            .queryParam("lng", longitude)
                            .build())
                    .header("Authorization", "Bearer " + apiToken)
                    .retrieve()
                    .body(QuickCommerceRawResponse.class);

            if (response == null || response.offers() == null) {
                log.warn("QuickCommerceAPI returned empty payload for query '{}'", query);
                return List.of();
            }

            return response.offers().stream()
                    .map(this::normalizeOffer)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch product offers from QuickCommerceAPI: {}", e.getMessage());
            throw new RuntimeException("QuickCommerceAPI search failed", e);
        }
    }

    @Override
    public boolean supports(String providerCode) {
        if (providerCode == null) return false;
        String normalized = providerCode.trim().toLowerCase();
        return "quickcommerce_api".equals(normalized) ||
                "quickcommerce-api".equals(normalized) ||
                "quickcommerce".equals(normalized) ||
                "api".equals(normalized) ||
                "all".equals(normalized);
    }

    @Override
    public String getProviderCode() {
        return "QUICKCOMMERCE_API";
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.QUICK_COMMERCE;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public long getTimeoutMs() {
        return timeoutMs;
    }

    private NormalizedProductOffer normalizeOffer(QuickCommerceRawOffer raw) {
        BigDecimal price = raw.price() != null ? raw.price() : BigDecimal.ZERO;
        BigDecimal mrp = raw.mrp() != null ? raw.mrp() : price;

        BigDecimal discountPercentage = BigDecimal.ZERO;
        if (mrp.compareTo(BigDecimal.ZERO) > 0 && mrp.compareTo(price) > 0) {
            discountPercentage = mrp.subtract(price)
                    .multiply(new BigDecimal("100"))
                    .divide(mrp, 2, RoundingMode.HALF_UP);
        }

        String platformCode = raw.platformCode() != null ? raw.platformCode().toUpperCase() : "UNKNOWN";
        String platformName = raw.platformName() != null ? raw.platformName() : platformCode;
        boolean inStock = raw.inStock() != null && raw.inStock();
        Integer etaMinutes = raw.etaMinutes() != null ? raw.etaMinutes() : 15;

        return new NormalizedProductOffer(
                platformCode,
                platformName,
                PlatformType.QUICK_COMMERCE,
                price,
                mrp,
                discountPercentage,
                inStock,
                DeliveryEstimate.instant(etaMinutes),
                platformName + " Hub",
                raw.productUrl(),
                raw.imageUrl()
        );
    }
}
