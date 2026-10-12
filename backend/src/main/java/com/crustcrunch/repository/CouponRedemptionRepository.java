package com.crustcrunch.repository;

import com.crustcrunch.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    @Query("""
            SELECT count(r) FROM CouponRedemption r
            WHERE r.coupon.id = :couponId
              AND ((:userId IS NOT NULL AND r.user.id = :userId) OR lower(r.contactEmail) = lower(:email))
            """)
    long countForCustomer(@Param("couponId") Long couponId, @Param("userId") Long userId, @Param("email") String email);

    void deleteByOrderId(Long orderId);
}
