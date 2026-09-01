package com.quickbasket.service.provider;

import com.quickbasket.dto.DeliveryEstimate;
import com.quickbasket.dto.NormalizedProductOffer;
import com.quickbasket.dto.PlatformType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mock implementation of ProductProvider supplying deterministic sample data.
 */
@Component
public class MockProductProvider implements ProductProvider {

    private final boolean enabled;
    private final long timeoutMs;

    public MockProductProvider() {
        this(true, 1500L);
    }

    public MockProductProvider(
            @Value("${quickbasket.providers.mock.enabled:true}") boolean enabled,
            @Value("${quickbasket.providers.mock.timeout-ms:1500}") long timeoutMs
    ) {
        this.enabled = enabled;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public List<NormalizedProductOffer> searchProducts(String query, String latitude, String longitude) {
        if (query != null && query.toLowerCase().contains("bread")) {
            return List.of(
                    createOffer("BLINKIT", "Blinkit", "38.00", "40.00", "5.00", true, 12, "https://blinkit.com/item/bread-1", "https://cdn.blinkit.com/bread.jpg"),
                    createOffer("ZEPTO", "Zepto", "36.00", "40.00", "10.00", true, 10, "https://zepto.com/item/bread-1", "https://cdn.zepto.com/bread.jpg")
            );
        }

        // Default milk offers
        return List.of(
                createOffer("BLINKIT", "Blinkit", "54.00", "56.00", "3.57", true, 14, "https://blinkit.com/item/amul-taaza-1l", "https://cdn.blinkit.com/img.jpg"),
                createOffer("ZEPTO", "Zepto", "53.00", "56.00", "5.36", true, 10, "https://zepto.com/item/amul-taaza-1l", "https://cdn.zepto.com/img.jpg"),
                createOffer("INSTAMART", "Swiggy Instamart", "55.00", "56.00", "1.79", true, 18, "https://swiggy.com/instamart/item/amul-taaza-1l", "https://cdn.instamart.com/img.jpg")
        );
    }

    @Override
    public boolean supports(String providerCode) {
        return "mock".equalsIgnoreCase(providerCode) || "all".equalsIgnoreCase(providerCode);
    }

    @Override
    public String getProviderCode() {
        return "MOCK";
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

    private NormalizedProductOffer createOffer(
            String code, String name, String price, String mrp, String discount,
            boolean inStock, int eta, String url, String imgUrl
    ) {
        return new NormalizedProductOffer(
                code,
                name,
                PlatformType.QUICK_COMMERCE,
                new BigDecimal(price),
                new BigDecimal(mrp),
                new BigDecimal(discount),
                inStock,
                DeliveryEstimate.instant(eta),
                name + " Warehouse",
                url,
                imgUrl
        );
    }
}
