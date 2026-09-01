package com.quickbasket.repository;

import com.quickbasket.entity.ProductEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("save and findByNameIgnoreCase should persist and retrieve product correctly")
    void saveAndFind_ShouldPersistAndRetrieveProduct() {
        ProductEntity product = new ProductEntity(
                "Amul Taaza Milk 1L",
                "Amul",
                "Dairy",
                "1",
                "L",
                "http://img"
        );

        ProductEntity saved = productRepository.save(product);
        assertThat(saved.getId()).isNotNull();

        Optional<ProductEntity> found = productRepository.findByNameIgnoreCase("Amul Taaza Milk 1L");
        assertThat(found).isPresent();
        assertThat(found.get().getBrand()).isEqualTo("Amul");
    }
}
