package com.quickbasket.service;

import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.entity.PlatformEntity;
import com.quickbasket.entity.PlatformOfferEntity;
import com.quickbasket.entity.PriceHistoryEntity;
import com.quickbasket.entity.ProductEntity;
import com.quickbasket.repository.PlatformOfferRepository;
import com.quickbasket.repository.PlatformRepository;
import com.quickbasket.repository.PriceHistoryRepository;
import com.quickbasket.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service handling product matching, platform resolution, live offer snapshot upserts,
 * and timeseries price history persistence.
 */
@Service
public class ProductCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogService.class);

    private final ProductRepository productRepository;
    private final PlatformRepository platformRepository;
    private final PlatformOfferRepository platformOfferRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public ProductCatalogService(
            ProductRepository productRepository,
            PlatformRepository platformRepository,
            PlatformOfferRepository platformOfferRepository,
            PriceHistoryRepository priceHistoryRepository
    ) {
        this.productRepository = productRepository;
        this.platformRepository = platformRepository;
        this.platformOfferRepository = platformOfferRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    /**
     * Persist or update normalized product offers and log timeseries price history entries.
     *
     * @param query  The user search query
     * @param offers List of normalized offers returned by the provider
     */
    @Transactional
    public void saveOffers(String query, List<NormalizedProductOffer> offers) {
        if (offers == null || offers.isEmpty()) {
            return;
        }

        for (NormalizedProductOffer offer : offers) {
            try {
                PlatformEntity platform = resolveOrCreatePlatform(offer.platformCode(), offer.platformName());
                ProductEntity product = matchOrCreateProduct(query, offer);
                String externalItemId = extractExternalItemId(offer);

                upsertPlatformOffer(product, platform, externalItemId, offer);
                recordPriceHistory(product, platform, offer);

            } catch (Exception e) {
                log.error("Failed to persist offer for platform {} and query '{}': {}", offer.platformCode(), query, e.getMessage(), e);
            }
        }
    }

    private PlatformEntity resolveOrCreatePlatform(String platformCode, String platformName) {
        String code = (platformCode != null ? platformCode : "UNKNOWN").toUpperCase();
        String name = (platformName != null ? platformName : code);

        return platformRepository.findByCode(code)
                .orElseGet(() -> platformRepository.save(new PlatformEntity(code, name)));
    }

    /**
     * Deterministic product matching according to ADR-010:
     * Matches by brand + normalized name where available, or falls back to canonical search query.
     */
    private ProductEntity matchOrCreateProduct(String query, NormalizedProductOffer offer) {
        String productName = extractProductName(query, offer);
        String brand = extractBrand(query, offer);

        return productRepository.findByNameIgnoreCaseAndBrandIgnoreCase(productName, brand)
                .or(() -> productRepository.findByNameIgnoreCase(productName))
                .orElseGet(() -> productRepository.save(new ProductEntity(
                        productName,
                        brand,
                        "General",
                        null,
                        null,
                        offer.imageUrl()
                )));
    }

    private void upsertPlatformOffer(
            ProductEntity product,
            PlatformEntity platform,
            String externalItemId,
            NormalizedProductOffer offer
    ) {
        PlatformOfferEntity platformOffer = platformOfferRepository
                .findByPlatformIdAndExternalItemId(platform.getId(), externalItemId)
                .orElseGet(() -> new PlatformOfferEntity(
                        product,
                        platform,
                        externalItemId,
                        offer.price(),
                        offer.mrp(),
                        offer.discountPercentage(),
                        offer.inStock(),
                        offer.etaMinutes(),
                        offer.productUrl()
                ));

        // Update live snapshot fields
        platformOffer.setProduct(product);
        platformOffer.setPrice(offer.price());
        platformOffer.setMrp(offer.mrp());
        platformOffer.setDiscountPercentage(offer.discountPercentage());
        platformOffer.setInStock(offer.inStock());
        platformOffer.setEtaMinutes(offer.etaMinutes());
        platformOffer.setProductUrl(offer.productUrl());
        platformOffer.setUpdatedAt(LocalDateTime.now());

        platformOfferRepository.save(platformOffer);
    }

    private void recordPriceHistory(ProductEntity product, PlatformEntity platform, NormalizedProductOffer offer) {
        PriceHistoryEntity history = new PriceHistoryEntity(
                product,
                platform,
                offer.price(),
                offer.mrp(),
                offer.inStock()
        );
        priceHistoryRepository.save(history);
    }

    private String extractExternalItemId(NormalizedProductOffer offer) {
        if (offer.productUrl() != null && !offer.productUrl().isBlank()) {
            return String.valueOf(Math.abs(offer.productUrl().hashCode()));
        }
        return offer.platformCode() + "_" + Math.abs(offer.price().hashCode());
    }

    private String extractProductName(String query, NormalizedProductOffer offer) {
        return query != null && !query.isBlank() ? query.trim() : "Standard Item";
    }

    private String extractBrand(String query, NormalizedProductOffer offer) {
        if (query != null) {
            String q = query.trim();
            int spaceIdx = q.indexOf(' ');
            if (spaceIdx > 0) {
                return q.substring(0, spaceIdx);
            }
            return q;
        }
        return "Generic";
    }
}
