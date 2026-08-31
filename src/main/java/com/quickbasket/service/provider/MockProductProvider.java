package com.quickbasket.service.provider;

import com.quickbasket.dto.NormalizedProductOffer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Offline, deterministic mock implementation of ProductProvider.
 * Used for local development, UI testing, and unit tests without external API dependencies.
 */
@Component
public class MockProductProvider implements ProductProvider {

    public static final String PROVIDER_CODE = "mock";

    @Override
    public List<NormalizedProductOffer> searchProducts(String query, String latitude, String longitude) {
        String queryLower = query != null ? query.toLowerCase().trim() : "";

        if (queryLower.contains("bread")) {
            return getMockBreadOffers();
        }

        // Default mock offers (e.g. Milk)
        return getMockMilkOffers(query);
    }

    @Override
    public boolean supports(String providerCode) {
        return PROVIDER_CODE.equalsIgnoreCase(providerCode);
    }

    private List<NormalizedProductOffer> getMockMilkOffers(String query) {
        return List.of(
                createOffer(
                        "BLINKIT",
                        "Blinkit",
                        new BigDecimal("54.00"),
                        new BigDecimal("56.00"),
                        true,
                        14,
                        "https://blinkit.com/prn/amul-taaza/prid/123",
                        "https://cdn.blinkit.com/images/amul_taaza.jpg"
                ),
                createOffer(
                        "ZEPTO",
                        "Zepto",
                        new BigDecimal("56.00"),
                        new BigDecimal("56.00"),
                        true,
                        10,
                        "https://zeptonow.com/pn/amul-taaza/id/456",
                        "https://cdn.zepto.com/images/amul_taaza.jpg"
                ),
                createOffer(
                        "INSTAMART",
                        "Swiggy Instamart",
                        new BigDecimal("55.00"),
                        new BigDecimal("56.00"),
                        true,
                        12,
                        "https://swiggy.com/instamart/item/789",
                        "https://cdn.swiggy.com/images/amul_taaza.jpg"
                )
        );
    }

    private List<NormalizedProductOffer> getMockBreadOffers() {
        return List.of(
                createOffer(
                        "BLINKIT",
                        "Blinkit",
                        new BigDecimal("40.00"),
                        new BigDecimal("45.00"),
                        true,
                        11,
                        "https://blinkit.com/prn/brown-bread/prid/101",
                        "https://cdn.blinkit.com/images/brown_bread.jpg"
                ),
                createOffer(
                        "ZEPTO",
                        "Zepto",
                        new BigDecimal("42.00"),
                        new BigDecimal("45.00"),
                        true,
                        8,
                        "https://zeptonow.com/pn/brown-bread/id/202",
                        "https://cdn.zepto.com/images/brown_bread.jpg"
                )
        );
    }

    private NormalizedProductOffer createOffer(
            String platformCode,
            String platformName,
            BigDecimal price,
            BigDecimal mrp,
            boolean inStock,
            Integer etaMinutes,
            String productUrl,
            String imageUrl
    ) {
        BigDecimal discountPct = BigDecimal.ZERO;
        if (mrp.compareTo(BigDecimal.ZERO) > 0 && price.compareTo(mrp) < 0) {
            discountPct = mrp.subtract(price)
                    .divide(mrp, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new NormalizedProductOffer(
                platformCode,
                platformName,
                price,
                mrp,
                discountPct,
                inStock,
                etaMinutes,
                productUrl,
                imageUrl
        );
    }
}
