package com.quickbasket.service.provider;

import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.QuickCommerceRawOffer;
import com.quickbasket.dto.QuickCommerceRawResponse;
import com.quickbasket.exception.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * External API provider calling QuickCommerceAPI aggregator using Spring 3.2 RestClient.
 */
@Component
public class QuickCommerceApiProvider implements ProductProvider {

    private static final Logger log = LoggerFactory.getLogger(QuickCommerceApiProvider.class);
    public static final String PROVIDER_CODE = "quickcommerce";

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;

    public QuickCommerceApiProvider(
            RestClient.Builder restClientBuilder,
            @Value("${quickcommerce.api.base-url:https://quickcommerceapi.com}") String baseUrl,
            @Value("${quickcommerce.api.key:mock-key}") String apiKey
    ) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public List<NormalizedProductOffer> searchProducts(String query, String latitude, String longitude) {
        log.info("Fetching products from QuickCommerce API at {} for query: '{}'", baseUrl, query);
        try {
            QuickCommerceRawResponse rawResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/search")
                            .queryParam("q", query)
                            .queryParam("lat", latitude)
                            .queryParam("lng", longitude)
                            .build())
                    .retrieve()
                    .body(QuickCommerceRawResponse.class);

            if (rawResponse == null || rawResponse.offers() == null) {
                return Collections.emptyList();
            }

            return rawResponse.offers().stream()
                    .map(this::normalizeOffer)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch product offers from QuickCommerce API: {}", e.getMessage());
            throw new ProviderException("QuickCommerce API call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean supports(String providerCode) {
        return PROVIDER_CODE.equalsIgnoreCase(providerCode);
    }

    private NormalizedProductOffer normalizeOffer(QuickCommerceRawOffer raw) {
        BigDecimal price = raw.price() != null ? raw.price() : BigDecimal.ZERO;
        BigDecimal mrp = raw.mrp() != null ? raw.mrp() : price;

        BigDecimal discountPct = BigDecimal.ZERO;
        if (mrp.compareTo(BigDecimal.ZERO) > 0 && price.compareTo(mrp) < 0) {
            discountPct = mrp.subtract(price)
                    .divide(mrp, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new NormalizedProductOffer(
                raw.platformCode() != null ? raw.platformCode() : "UNKNOWN",
                raw.platformName() != null ? raw.platformName() : "Unknown Platform",
                price,
                mrp,
                discountPct,
                Boolean.TRUE.equals(raw.inStock()),
                raw.etaMinutes() != null ? raw.etaMinutes() : 999,
                raw.productUrl(),
                raw.imageUrl()
        );
    }
}
