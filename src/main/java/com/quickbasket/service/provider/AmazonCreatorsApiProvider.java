package com.quickbasket.service.provider;

import com.quickbasket.dto.DeliveryEstimate;
import com.quickbasket.dto.DeliveryType;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import com.quickbasket.dto.amazon.AmazonAvailability;
import com.quickbasket.dto.amazon.AmazonImageContainer;
import com.quickbasket.dto.amazon.AmazonItem;
import com.quickbasket.dto.amazon.AmazonListing;
import com.quickbasket.dto.amazon.AmazonSearchRequest;
import com.quickbasket.dto.amazon.AmazonSearchResponse;
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
import java.util.Objects;

/**
 * Official Amazon Creators API provider integration (/catalog/v1/searchItems).
 * Represents an external e-commerce integration source (AMAZON).
 */
@Component
public class AmazonCreatorsApiProvider implements ProductProvider {

    private static final Logger log = LoggerFactory.getLogger(AmazonCreatorsApiProvider.class);

    private static final List<String> REQUESTED_RESOURCES = List.of(
            "itemInfo.title",
            "images.primary.medium",
            "offersV2.listings.price",
            "offersV2.listings.savingBasis",
            "offersV2.listings.availability",
            "offersV2.listings.merchantInfo"
    );

    private final RestClient restClient;
    private final AmazonTokenService tokenService;
    private final boolean enabled;
    private final String clientId;
    private final String clientSecret;
    private final String partnerTag;
    private final String marketplace;
    private final long timeoutMs;
    private final int itemCount;

    public AmazonCreatorsApiProvider(
            RestClient.Builder restClientBuilder,
            AmazonTokenService tokenService,
            @Value("${quickbasket.providers.amazon.enabled:false}") boolean enabled,
            @Value("${quickbasket.providers.amazon.client-id:}") String clientId,
            @Value("${quickbasket.providers.amazon.client-secret:}") String clientSecret,
            @Value("${quickbasket.providers.amazon.partner-tag:}") String partnerTag,
            @Value("${quickbasket.providers.amazon.marketplace:www.amazon.in}") String marketplace,
            @Value("${quickbasket.providers.amazon.base-url:https://creatorsapi.amazon}") String baseUrl,
            @Value("${quickbasket.providers.amazon.timeout-ms:1500}") long timeoutMs,
            @Value("${quickbasket.providers.amazon.item-count:10}") int itemCount
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.tokenService = tokenService;
        this.enabled = enabled;
        this.clientId = clientId != null ? clientId.trim() : "";
        this.clientSecret = clientSecret != null ? clientSecret.trim() : "";
        this.partnerTag = partnerTag != null ? partnerTag.trim() : "";
        this.marketplace = marketplace != null ? marketplace.trim() : "www.amazon.in";
        this.timeoutMs = timeoutMs;
        this.itemCount = itemCount;
    }

    @Override
    public List<NormalizedProductOffer> searchProducts(String query, String latitude, String longitude) {
        if (!isEnabled()) {
            log.info("AmazonCreatorsApiProvider is disabled or credentials missing. Skipping search.");
            return List.of();
        }

        log.info("Fetching Amazon Creators API offers for query '{}' (marketplace: {})", query, marketplace);

        String accessToken = tokenService.getAccessToken();

        AmazonSearchRequest requestBody = new AmazonSearchRequest(
                query,
                partnerTag,
                marketplace,
                itemCount,
                REQUESTED_RESOURCES
        );

        try {
            AmazonSearchResponse response = restClient.post()
                    .uri("/catalog/v1/searchItems")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .header("x-marketplace", marketplace)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.value() == 400, (req, resp) -> {
                        throw new ProviderException("Amazon Creators API bad request: HTTP 400");
                    })
                    .onStatus(status -> status.value() == 401 || status.value() == 403, (req, resp) -> {
                        throw new ProviderException("Amazon Creators API authentication failed: HTTP " + resp.getStatusCode());
                    })
                    .onStatus(status -> status.value() == 429, (req, resp) -> {
                        throw new ProviderException("Amazon Creators API rate limit exceeded: HTTP 429");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                        throw new ProviderException("Amazon Creators API server error: HTTP " + resp.getStatusCode());
                    })
                    .body(AmazonSearchResponse.class);

            if (response == null || response.searchResult() == null || response.searchResult().items() == null) {
                log.warn("Amazon Creators API returned empty search result for query '{}'", query);
                return List.of();
            }

            return response.searchResult().items().stream()
                    .map(this::normalizeItem)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden e) {
            log.error("Amazon Creators API authentication failure: {}", e.getMessage());
            throw new ProviderException("Amazon Creators API authentication failed", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("Amazon Creators API rate limit exceeded: {}", e.getMessage());
            throw new ProviderException("Amazon Creators API rate limit exceeded", e);
        } catch (ResourceAccessException e) {
            log.warn("Amazon Creators API connection failure/timeout: {}", e.getMessage());
            throw new ProviderException("Amazon Creators API connection/timeout failure", e);
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error invoking Amazon Creators API: {}", e.getMessage());
            throw new ProviderException("Amazon Creators API unexpected error", e);
        }
    }

    @Override
    public boolean supports(String providerCode) {
        if (providerCode == null) return false;
        String normalized = providerCode.trim().toLowerCase();
        return "amazon".equals(normalized) || "all".equals(normalized);
    }

    @Override
    public String getProviderCode() {
        return "AMAZON";
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.ECOMMERCE;
    }

    @Override
    public boolean isEnabled() {
        return enabled && !clientId.isBlank() && !clientSecret.isBlank() && !partnerTag.isBlank();
    }

    @Override
    public long getTimeoutMs() {
        return timeoutMs;
    }

    private NormalizedProductOffer normalizeItem(AmazonItem item) {
        if (item == null) {
            return null;
        }

        AmazonListing listing = selectListing(item);
        if (listing == null || listing.price() == null) {
            return null;
        }

        BigDecimal price = (listing.price().price() != null && listing.price().price().amount() != null)
                ? listing.price().price().amount()
                : (listing.price().savingBasis() != null ? listing.price().savingBasis().amount() : BigDecimal.ZERO);

        BigDecimal mrp = (listing.price().savingBasis() != null && listing.price().savingBasis().amount() != null)
                ? listing.price().savingBasis().amount()
                : price;

        if (price.compareTo(BigDecimal.ZERO) <= 0 && mrp.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal discountPercentage = BigDecimal.ZERO;
        if (listing.price().savingsPercentage() != null) {
            discountPercentage = BigDecimal.valueOf(listing.price().savingsPercentage()).setScale(2, RoundingMode.HALF_UP);
        } else if (mrp.compareTo(BigDecimal.ZERO) > 0 && mrp.compareTo(price) > 0) {
            discountPercentage = mrp.subtract(price)
                    .multiply(new BigDecimal("100"))
                    .divide(mrp, 2, RoundingMode.HALF_UP);
        }

        boolean inStock = isAvailableInStock(listing.availability());

        String sellerName = (listing.merchantInfo() != null && listing.merchantInfo().name() != null && !listing.merchantInfo().name().isBlank())
                ? listing.merchantInfo().name()
                : "Amazon Seller";

        String imageUrl = resolveImageUrl(item);

        DeliveryEstimate delivery = new DeliveryEstimate(
                DeliveryType.STANDARD,
                null,
                null,
                null
        );

        return new NormalizedProductOffer(
                "AMAZON",
                "Amazon",
                PlatformType.ECOMMERCE,
                price,
                mrp,
                discountPercentage,
                inStock,
                delivery,
                sellerName,
                item.detailPageURL(),
                imageUrl
        );
    }

    private AmazonListing selectListing(AmazonItem item) {
        if (item.offersV2() == null || item.offersV2().listings() == null || item.offersV2().listings().isEmpty()) {
            return null;
        }
        return item.offersV2().listings().stream()
                .filter(l -> Boolean.TRUE.equals(l.isBuyBoxWinner()))
                .findFirst()
                .orElseGet(() -> item.offersV2().listings().get(0));
    }

    private boolean isAvailableInStock(AmazonAvailability availability) {
        if (availability == null || availability.type() == null) {
            return false;
        }
        String type = availability.type().trim().toUpperCase();
        return "IN_STOCK".equals(type) || "INSTOCK".equals(type) || "AVAILABLE".equals(type);
    }

    private String resolveImageUrl(AmazonItem item) {
        if (item.images() == null || item.images().primary() == null) {
            return null;
        }
        AmazonImageContainer container = item.images().primary();
        if (container.medium() != null && container.medium().url() != null) {
            return container.medium().url();
        }
        if (container.large() != null && container.large().url() != null) {
            return container.large().url();
        }
        if (container.small() != null && container.small().url() != null) {
            return container.small().url();
        }
        return null;
    }
}
