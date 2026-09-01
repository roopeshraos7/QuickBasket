package com.quickbasket.repository;

import com.quickbasket.entity.PriceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistoryEntity, Long> {

    List<PriceHistoryEntity> findByProductIdAndPlatformIdOrderByRecordedAtDesc(Long productId, Integer platformId);

    List<PriceHistoryEntity> findByProductIdOrderByRecordedAtDesc(Long productId);
}
