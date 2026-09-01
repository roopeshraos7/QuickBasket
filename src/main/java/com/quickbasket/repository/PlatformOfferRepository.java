package com.quickbasket.repository;

import com.quickbasket.entity.PlatformOfferEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformOfferRepository extends JpaRepository<PlatformOfferEntity, Long> {

    Optional<PlatformOfferEntity> findByPlatformIdAndExternalItemId(Integer platformId, String externalItemId);

    List<PlatformOfferEntity> findByProductId(Long productId);
}
