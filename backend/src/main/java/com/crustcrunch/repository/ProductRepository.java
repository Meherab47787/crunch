package com.crustcrunch.repository;

import com.crustcrunch.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    long countByCategoryId(Long categoryId);

    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id IN :ids")
    List<Product> findAllWithCategoryByIdIn(@Param("ids") Collection<Long> ids);

    @Query("SELECT p.category.id, count(p) FROM Product p WHERE p.visible = true GROUP BY p.category.id")
    List<Object[]> countVisibleByCategory();

    @Query("SELECT p.category.id, count(p) FROM Product p GROUP BY p.category.id")
    List<Object[]> countAllByCategory();

    @Modifying
    @Query("UPDATE Product p SET p.soldCount = p.soldCount + :qty WHERE p.id = :id")
    void incrementSold(@Param("id") Long id, @Param("qty") int qty);
}
