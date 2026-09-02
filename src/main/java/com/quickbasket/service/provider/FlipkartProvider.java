package com.quickbasket.service.provider;

import com.quickbasket.dto.DeliveryEstimate;
import com.quickbasket.dto.DeliveryType;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import com.quickbasket.dto.flipkart.FlipkartProductAttributes;
import com.quickbasket.dto.flipkart.FlipkartProductBaseInfoV1;
import com.quickbasket.dto.flipkart.FlipkartProductWrapper;
import com.quickbasket.dto.flipkart.FlipkartSearchResponse;
import com.quickbasket.dto.flipkart.FlipkartShippingInfo;
import com.quickbasket.exception.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Official Flipkart Affiliate v1.0 API provider integration (/affiliate/1.0/search.json).
 * Represents an external e-commerce integration source (FLIPKART).
 */
@Component
public class FlipkartProvider implements ProductProvider {

    private static final Logger log = LoggerFactory.getLogger(FlipkartProvider.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String affiliateId;
    private final String affiliateToken;
    private final long timeoutMs;
    private final int resultCount;

    public FlipkartProvider(
            RestClient.Builder restClientBuilder,
            @Value("${quickbasket.providers.flipkart.enabled:false}") boolean enabled,
            @Value("${quickbasket.providers.flipkart.affiliate-id:}") String affiliateId,
            @Value("${quickbasket.providers.flipkart.affiliate-token:}") String affiliateToken,
            @Value("${quickbasket.providers.flipkart.base-url:https://affiliate-api.flipkart.net}") String baseUrl,
            @Value("${quickbasket.providers.flipkart.timeout-ms:2000}") long timeoutMs,
            @Value("${quickbasket.providers.flipkart.result-count:10}") int resultCount
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.enabled = enabled;
        this.affiliateId = affiliateId != null ? affiliateId.trim() : "";
        this.affiliateToken = affiliateToken != null ? affiliateToken.trim() : "";
        this.timeoutMs = timeoutMs;
        this.resultCount = resultCount;
    }

    @Override
    public List<NormalizedProductOffer> searchProducts(String query, String latitude, String longitude) {
        if (!isEnabled()) {
            log.info("FlipkartProvider is disabled or credentials missing. Skipping search.");
            return List.of();
        }

        log.info("Fetching Flipkart v1.0 offers for query '{}'", query);

        try {
            FlipkartSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/affiliate/1.0/search.json")
                            .queryParam("query", query)
                            .queryParam("resultCount", resultCount)
                            .build())
                    .header("Fk-Affiliate-Id", affiliateId)
                    .header("Fk-Affiliate-Token", affiliateToken)
                    .header("Accept", "application/json")
                    .retrieve()
                    .onStatus(status -> status.value() == 401 || status.value() == 403, (req, resp) -> {
                        throw new ProviderException("Flipkart API authentication failed: HTTP " + resp.getStatusCode());
                    })
                    .onStatus(status -> status.value() == 429, (req, resp) -> {
                        throw new ProviderException("Flipkart API rate limit exceeded: HTTP 429");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new ProviderException("Flipkart API server error: HTTP " + resp.getStatusCode());
                    })
                    .body(FlipkartSearchResponse.class);

            if (response == null || response.productInfoList() == null) {
                log.warn("Flipkart API returned empty response payload for query '{}'", query);
                return List.of();
            }

            return response.productInfoList().stream()
                    .map(this::normalizeProduct)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.error("Flipkart API authentication failure: {}", e.getMessage());
            throw new ProviderException("Flipkart API authentication failed", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Flipkart API rate limit exceeded: {}", e.getMessage());
            throw new ProviderException("Flipkart API rate limit exceeded", e);
        } catch (ResourceAccessException e) {
            log.warn("Flipkart API connection failure/timeout: {}", e.getMessage());
            throw new ProviderException("Flipkart API connection/timeout failure", e);
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error invoking Flipkart API: {}", e.getMessage());
            throw new ProviderException("Flipkart API unexpected error", e);
        }
    }

    @Override
    public boolean supports(String providerCode) {
        if (providerCode == null) return false;
        String normalized = providerCode.trim().toLowerCase();
        return "flipkart".equals(normalized) || "all".equals(normalized);
    }

    @Override
    public String getProviderCode() {
        return "FLIPKART";
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.ECOMMERCE;
    }

    @Override
    public boolean isEnabled() {
        return enabled && !affiliateId.isBlank() && !affiliateToken.isBlank();
    }

    @Override
    public long getTimeoutMs() {
        return timeoutMs;
    }

    private NormalizedProductOffer normalizeProduct(FlipkartProductWrapper wrapper) {
        if (wrapper == null || wrapper.productBaseInfoV1() == null) {
            return null;
        }

        FlipkartProductBaseInfoV1 baseInfo = wrapper.productBaseInfoV1();
        FlipkartProductAttributes attrs = baseInfo.productAttributes();
        if (attrs == null) {
            return null;
        }

        BigDecimal price = (attrs.sellingPrice() != null && attrs.sellingPrice().amount() != null)
                ? attrs.sellingPrice().amount()
                : (attrs.maximumRetailPrice() != null ? attrs.maximumRetailPrice().amount() : BigDecimal.ZERO);

        BigDecimal mrp = (attrs.maximumRetailPrice() != null && attrs.maximumRetailPrice().amount() != null)
                ? attrs.maximumRetailPrice().amount()
                : price;

        if (price.compareTo(BigDecimal.ZERO) <= 0 && mrp.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal discountPercentage = BigDecimal.ZERO;
        if (attrs.discountPercentage() != null) {
            discountPercentage = BigDecimal.valueOf(attrs.discountPercentage()).setScale(2, RoundingMode.HALF_UP);
        } else if (mrp.compareTo(BigDecimal.ZERO) > 0 && mrp.compareTo(price) > 0) {
            discountPercentage = mrp.subtract(price)
                    .multiply(new BigDecimal("100"))
                    .divide(mrp, 2, RoundingMode.HALF_UP);
        }

        boolean inStock = (attrs.inStock() == null || attrs.inStock())
                && (attrs.isAvailable() == null || attrs.isAvailable());

        FlipkartShippingInfo shipping = attrs.shippingInfo();
        BigDecimal shippingFee = (shipping != null && shipping.shippingFees() != null && shipping.shippingFees().amount() != null)
                ? shipping.shippingFees().amount()
                : BigDecimal.ZERO;

        String deliveryText = (shipping != null && shipping.estimatedDelivery() != null && !shipping.estimatedDelivery().isBlank())
                ? shipping.estimatedDelivery()
                : "Standard delivery in 2-4 business days";

        DeliveryEstimate delivery = new DeliveryEstimate(
                DeliveryType.STANDARD,
                null,
                deliveryText,
                shippingFee
        );

        String sellerName = (attrs.sellerName() != null && !attrs.sellerName().isBlank())
                ? attrs.sellerName()
                : "Flipkart Seller";

        String imageUrl = resolveImageUrl(attrs.imageUrls());

        return new NormalizedProductOffer(
                "FLIPKART",
                "Flipkart",
                PlatformType.ECOMMERCE,
                price,
                mrp,
                discountPercentage,
                inStock,
                delivery,
                sellerName,
                attrs.productUrl(),
                imageUrl
        );
    }

    private String resolveImageUrl(Map<String, String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        if (imageUrls.containsKey("400x400") && imageUrls.get("400x400") != null) {
            return imageUrls.get("400x400");
        }
        if (imageUrls.containsKey("200x200") && imageUrls.get("200x200") != null) {
            return imageUrls.get("200x200");
        }
        return imageUrls.values().stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
