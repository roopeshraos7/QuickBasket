package com.quickbasket.repository;

import com.quickbasket.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findByNameIgnoreCaseAndBrandIgnoreCase(String name, String brand);

    Optional<ProductEntity> findByNameIgnoreCase(String name);
}
