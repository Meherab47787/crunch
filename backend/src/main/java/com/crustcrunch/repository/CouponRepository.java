package com.crustcrunch.repository;

import com.crustcrunch.entity.Coupon;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    @Query("SELECT c FROM Coupon c WHERE upper(c.code) = upper(:code)")
    Optional<Coupon> findByCode(@Param("code") String code);

    /** Row lock used at checkout so redemption limits can't be exceeded by concurrent orders. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE upper(c.code) = upper(:code)")
    Optional<Coupon> findByCodeForUpdate(@Param("code") String code);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Coupon> findAllByOrderByCreatedAtDesc();
}
