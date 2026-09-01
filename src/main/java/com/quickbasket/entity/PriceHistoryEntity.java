package com.quickbasket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity representing append-only timeseries price observation logs.
 */
@Entity
@Table(
        name = "price_history",
        indexes = {
                @Index(name = "idx_price_history_lookup", columnList = "product_id, platform_id, recorded_at DESC")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PriceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platform_id", nullable = false)
    private PlatformEntity platform;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "mrp", nullable = false, precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(name = "in_stock", nullable = false)
    private boolean inStock = true;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    public PriceHistoryEntity(
            ProductEntity product,
            PlatformEntity platform,
            BigDecimal price,
            BigDecimal mrp,
            boolean inStock
    ) {
        this.product = product;
        this.platform = platform;
        this.price = price;
        this.mrp = mrp;
        this.inStock = inStock;
        this.recordedAt = LocalDateTime.now();
    }
}
