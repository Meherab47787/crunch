package com.crustcrunch.repository;

import com.crustcrunch.entity.Order;
import com.crustcrunch.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdempotencyKey(String key);

    Optional<Order> findByOrderNumber(String orderNumber);

    boolean existsByOrderNumberStartingWith(String prefix);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Order> findByStatusInOrderByCreatedAtAsc(Collection<OrderStatus> statuses);

    long countByUserId(Long userId);

    long countByStatus(OrderStatus status);

    long countByStatusIn(Collection<OrderStatus> statuses);

    @Query("SELECT count(o) FROM Order o WHERE o.createdAt >= :from AND o.status <> com.crustcrunch.enums.OrderStatus.CANCELLED")
    long countSince(@Param("from") Instant from);

    @Query("SELECT coalesce(sum(o.total), 0) FROM Order o WHERE o.createdAt >= :from AND o.status <> com.crustcrunch.enums.OrderStatus.CANCELLED")
    BigDecimal revenueSince(@Param("from") Instant from);

    @Query("SELECT coalesce(sum(o.total), 0) FROM Order o WHERE o.status <> com.crustcrunch.enums.OrderStatus.CANCELLED")
    BigDecimal totalRevenue();

    @Query("SELECT coalesce(sum(o.total), 0) FROM Order o WHERE o.user.id = :userId AND o.status <> com.crustcrunch.enums.OrderStatus.CANCELLED")
    BigDecimal totalSpentByUser(@Param("userId") Long userId);

    /** Daily buckets in the restaurant's time zone: [day, orders, revenue]. */
    @Query(value = """
            SELECT to_char(date_trunc(:unit, o.created_at AT TIME ZONE :tz), 'YYYY-MM-DD') AS bucket,
                   count(*) AS orders,
                   coalesce(sum(o.total), 0) AS revenue
            FROM orders o
            WHERE o.created_at >= :from AND o.status <> 'CANCELLED'
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> salesBuckets(@Param("unit") String unit, @Param("tz") String tz, @Param("from") Instant from);

    @Query(value = """
            SELECT oi.product_id, oi.product_name, sum(oi.quantity) AS qty, sum(oi.line_total) AS revenue
            FROM order_items oi JOIN orders o ON o.id = oi.order_id
            WHERE o.created_at >= :from AND o.status <> 'CANCELLED'
            GROUP BY oi.product_id, oi.product_name
            ORDER BY qty DESC
            LIMIT :lim
            """, nativeQuery = true)
    List<Object[]> topProducts(@Param("from") Instant from, @Param("lim") int limit);

    @Query(value = """
            SELECT oi.category_name, sum(oi.quantity) AS qty, sum(oi.line_total) AS revenue
            FROM order_items oi JOIN orders o ON o.id = oi.order_id
            WHERE o.created_at >= :from AND o.status <> 'CANCELLED'
            GROUP BY oi.category_name
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> categorySales(@Param("from") Instant from);

    @Query("""
            SELECT count(o) > 0 FROM Order o JOIN o.items i
            WHERE o.id = :orderId AND o.user.id = :userId AND i.productId = :productId
              AND o.status = com.crustcrunch.enums.OrderStatus.DELIVERED
            """)
    boolean isReviewEligible(@Param("orderId") Long orderId, @Param("userId") Long userId, @Param("productId") Long productId);
}
