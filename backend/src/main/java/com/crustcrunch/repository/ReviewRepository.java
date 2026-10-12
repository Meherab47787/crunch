package com.crustcrunch.repository;

import com.crustcrunch.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @EntityGraph(attributePaths = "user")
    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    List<Review> findByUserIdAndOrderId(Long userId, Long orderId);

    /** Highlighted reviews for the homepage: recent 4-5 star reviews with a comment. */
    @EntityGraph(attributePaths = {"user", "product"})
    @Query("SELECT r FROM Review r WHERE r.rating >= 4 AND r.comment IS NOT NULL AND length(r.comment) > 20 ORDER BY r.createdAt DESC")
    List<Review> findHighlights(Pageable pageable);
}
