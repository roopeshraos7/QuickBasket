package com.quickbasket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA Entity representing the canonical product catalog item.
 */
@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_search", columnList = "name, brand")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "quantity", length = 50)
    private String quantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ProductEntity(String name, String brand, String category, String quantity, String unit, String imageUrl) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.quantity = quantity;
        this.unit = unit;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDateTime.now();
    }
}
