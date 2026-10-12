package com.crustcrunch.service;

import com.crustcrunch.entity.Category;
import com.crustcrunch.entity.Product;
import com.crustcrunch.enums.ProductType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Dynamic filters for menu browsing and the admin product list. */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public record ProductFilter(String q, String category, BigDecimal minPrice, BigDecimal maxPrice,
                                Boolean vegetarian, Boolean spicy, Boolean availableOnly, String allergenFree,
                                Boolean featured, Boolean bestseller, Boolean newArrival, ProductType type,
                                boolean publicView) {
    }

    public static Specification<Product> of(ProductFilter f) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            Join<Product, Category> cat = root.join("category");
            if (f.publicView()) {
                ps.add(cb.isTrue(root.get("visible")));
                ps.add(cb.isTrue(cat.get("active")));
            }
            if (f.q() != null && !f.q().isBlank()) {
                String like = "%" + f.q().trim().toLowerCase().replace("%", "\\%").replace("_", "\\_") + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like, '\\'),
                        cb.like(cb.lower(root.get("description")), like, '\\'),
                        cb.like(cb.lower(root.get("ingredients")), like, '\\'),
                        cb.like(cb.lower(cat.get("name")), like, '\\')));
            }
            if (f.category() != null && !f.category().isBlank()) {
                ps.add(cb.equal(cat.get("slug"), f.category()));
            }
            if (f.minPrice() != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("basePrice"), f.minPrice()));
            }
            if (f.maxPrice() != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("basePrice"), f.maxPrice()));
            }
            if (f.vegetarian() != null) {
                ps.add(cb.equal(root.get("vegetarian"), f.vegetarian()));
            }
            if (f.spicy() != null) {
                ps.add(cb.equal(root.get("spicy"), f.spicy()));
            }
            if (Boolean.TRUE.equals(f.availableOnly())) {
                ps.add(cb.isTrue(root.get("available")));
            }
            if (f.allergenFree() != null && !f.allergenFree().isBlank()) {
                for (String allergen : f.allergenFree().split(",")) {
                    String a = allergen.trim().toLowerCase();
                    if (!a.isEmpty()) {
                        ps.add(cb.or(cb.isNull(root.get("allergens")),
                                cb.notLike(cb.lower(root.get("allergens")), "%" + a + "%")));
                    }
                }
            }
            if (Boolean.TRUE.equals(f.featured())) {
                ps.add(cb.isTrue(root.get("featured")));
            }
            if (Boolean.TRUE.equals(f.bestseller())) {
                ps.add(cb.isTrue(root.get("bestseller")));
            }
            if (Boolean.TRUE.equals(f.newArrival())) {
                ps.add(cb.isTrue(root.get("newArrival")));
            }
            if (f.type() != null) {
                ps.add(cb.equal(root.get("productType"), f.type()));
            }
            return cb.and(ps.toArray(Predicate[]::new));
        };
    }
}
