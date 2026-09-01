package com.quickbasket.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing a quick-commerce platform (e.g. Blinkit, Zepto, Swiggy Instamart).
 */
@Entity
@Table(name = "platforms")
@Getter
@Setter
@NoArgsConstructor
public class PlatformEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public PlatformEntity(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
        this.isActive = true;
    }
}
