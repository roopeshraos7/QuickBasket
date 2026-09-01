package com.quickbasket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity representing a live price and stock snapshot offer from a vendor platform.
 * Unique constraint enforces ADR-010 item identity: (platform_id, external_item_id).
 */
@Entity
@Table(
        name = "platform_offers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_platform_external_item", columnNames = {"platform_id", "external_item_id"})
        },
        indexes = {
                @Index(name = "idx_offers_product_id", columnList = "product_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PlatformOfferEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_id", nullable = false)
    private PlatformEntity platform;

    @Column(name = "external_item_id", nullable = false, length = 100)
    private String externalItemId;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "mrp", nullable = false, precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column(name = "in_stock", nullable = false)
    private boolean inStock = true;

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @Column(name = "product_url", columnDefinition = "TEXT")
    private String productUrl;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public PlatformOfferEntity(
            ProductEntity product,
            PlatformEntity platform,
            String externalItemId,
            BigDecimal price,
            BigDecimal mrp,
            BigDecimal discountPercentage,
            boolean inStock,
            Integer etaMinutes,
            String productUrl
    ) {
        this.product = product;
        this.platform = platform;
        this.externalItemId = externalItemId;
        this.price = price;
        this.mrp = mrp;
        this.discountPercentage = discountPercentage;
        this.inStock = inStock;
        this.etaMinutes = etaMinutes;
        this.productUrl = productUrl;
        this.updatedAt = LocalDateTime.now();
    }
}
