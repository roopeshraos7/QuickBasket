package com.quickbasket.repository;

import com.quickbasket.entity.PlatformEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PlatformRepository extends JpaRepository<PlatformEntity, Integer> {

    Optional<PlatformEntity> findByCode(String code);
}
