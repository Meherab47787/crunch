package com.crustcrunch.repository;

import com.crustcrunch.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("""
            SELECT p FROM Promotion p
            WHERE p.active = true
              AND (p.startsAt IS NULL OR p.startsAt <= :now)
              AND (p.endsAt IS NULL OR p.endsAt > :now)
            ORDER BY p.id
            """)
    List<Promotion> findLive(@Param("now") Instant now);

    List<Promotion> findAllByOrderByCreatedAtDesc();
}
