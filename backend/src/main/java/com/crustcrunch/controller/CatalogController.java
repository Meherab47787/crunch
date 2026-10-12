package com.crustcrunch.controller;

import com.crustcrunch.dto.MenuDtos.*;
import com.crustcrunch.dto.PageResponse;
import com.crustcrunch.enums.ProductType;
import com.crustcrunch.service.CatalogService;
import com.crustcrunch.service.ProductSpecifications.ProductFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Menu (public)")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return catalog.activeCategories();
    }

    @GetMapping("/categories/{slug}")
    public CategoryResponse category(@PathVariable String slug) {
        return catalog.category(slug);
    }

    @GetMapping("/products")
    public PageResponse<ProductSummary> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean vegetarian,
            @RequestParam(required = false) Boolean spicy,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String allergenFree,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean bestseller,
            @RequestParam(required = false) Boolean newArrival,
            @RequestParam(required = false) ProductType type,
            @RequestParam(defaultValue = "") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        ProductFilter filter = new ProductFilter(q, category, minPrice, maxPrice, vegetarian, spicy, available,
                allergenFree, featured, bestseller, newArrival, type, true);
        return catalog.search(filter, sort, page, size);
    }

    @GetMapping("/products/{slug}")
    public ProductDetail product(@PathVariable String slug) {
        return catalog.product(slug);
    }

    @GetMapping("/products/{slug}/reviews")
    public PageResponse<ReviewResponse> reviews(@PathVariable String slug,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return catalog.productReviews(slug, page, size);
    }

    @GetMapping("/reviews/highlights")
    public List<ReviewResponse> highlightedReviews(@RequestParam(defaultValue = "6") int limit) {
        return catalog.highlightedReviews(limit);
    }

    @GetMapping("/promotions")
    public List<PromotionResponse> promotions() {
        return catalog.livePromotions();
    }

    @GetMapping("/settings")
    public PublicSettings settings() {
        return catalog.settings();
    }
}
