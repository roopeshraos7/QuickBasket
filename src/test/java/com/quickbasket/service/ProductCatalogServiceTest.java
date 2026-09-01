package com.quickbasket.service;

import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.entity.PlatformOfferEntity;
import com.quickbasket.entity.PriceHistoryEntity;
import com.quickbasket.repository.PlatformOfferRepository;
import com.quickbasket.repository.PlatformRepository;
import com.quickbasket.repository.PriceHistoryRepository;
import com.quickbasket.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductCatalogServiceTest {

    @Autowired
    private ProductCatalogService catalogService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PlatformRepository platformRepository;

    @Autowired
    private PlatformOfferRepository platformOfferRepository;

    @Autowired
    private PriceHistoryRepository priceHistoryRepository;

    @Test
    @DisplayName("saveOffers should persist product, snapshot offer, and price history entry")
    void saveOffers_ShouldPersistEntitiesAndPriceHistory() {
        NormalizedProductOffer offer = new NormalizedProductOffer(
                "BLINKIT",
                "Blinkit",
                new BigDecimal("54.00"),
                new BigDecimal("56.00"),
                new BigDecimal("3.57"),
                true,
                14,
                "https://blinkit.com/item/123",
                "https://cdn.blinkit.com/img.jpg"
        );

        catalogService.saveOffers("Amul Taaza Milk 1L", List.of(offer));

        assertThat(productRepository.count()).isGreaterThanOrEqualTo(1);
        assertThat(platformRepository.findByCode("BLINKIT")).isPresent();

        List<PlatformOfferEntity> offers = platformOfferRepository.findAll();
        assertThat(offers).hasSize(1);
        assertThat(offers.get(0).getPrice()).isEqualTo(new BigDecimal("54.00"));

        List<PriceHistoryEntity> histories = priceHistoryRepository.findAll();
        assertThat(histories).hasSize(1);
        assertThat(histories.get(0).getPrice()).isEqualTo(new BigDecimal("54.00"));
    }
}
