package com.crustcrunch.service;

import com.crustcrunch.config.AppProperties;
import com.crustcrunch.dto.MenuDtos.*;
import com.crustcrunch.dto.PageResponse;
import com.crustcrunch.entity.Category;
import com.crustcrunch.entity.Product;
import com.crustcrunch.entity.Promotion;
import com.crustcrunch.exception.NotFoundException;
import com.crustcrunch.mapper.MenuMapper;
import com.crustcrunch.repository.CategoryRepository;
import com.crustcrunch.repository.ProductRepository;
import com.crustcrunch.repository.PromotionRepository;
import com.crustcrunch.repository.ReviewRepository;
import com.crustcrunch.service.ProductSpecifications.ProductFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Read-only storefront queries. */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final PromotionRepository promotions;
    private final ReviewRepository reviews;
    private final PricingService pricing;
    private final MenuMapper mapper;
    private final AppProperties props;
    private final Clock clock;

    public CatalogService(CategoryRepository categories, ProductRepository products, PromotionRepository promotions,
                          ReviewRepository reviews, PricingService pricing, MenuMapper mapper, AppProperties props,
                          Clock clock) {
        this.categories = categories;
        this.products = products;
        this.promotions = promotions;
        this.reviews = reviews;
        this.pricing = pricing;
        this.mapper = mapper;
        this.props = props;
        this.clock = clock;
    }

    public List<CategoryResponse> activeCategories() {
        Map<Long, Long> counts = products.countVisibleByCategory().stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return categories.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(c -> mapper.toResponse(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    public CategoryResponse category(String slug) {
        Category c = categories.findBySlug(slug).filter(Category::isActive)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        return mapper.toResponse(c, 0);
    }

    public PageResponse<ProductSummary> search(ProductFilter filter, String sort, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 60), sortFor(sort));
        Page<Product> result = products.findAll(ProductSpecifications.of(filter), pageable);
        List<Promotion> live = pricing.livePromotions();
        return PageResponse.of(result, p -> mapper.toSummary(p, live));
    }

    public ProductDetail product(String slug) {
        Product p = products.findBySlug(slug)
                .filter(x -> x.isVisible() && x.getCategory().isActive())
                .orElseThrow(() -> new NotFoundException("Product not found"));
        return mapper.toDetail(p, pricing.livePromotions());
    }

    public PageResponse<ReviewResponse> productReviews(String slug, int page, int size) {
        Product p = products.findBySlug(slug).orElseThrow(() -> new NotFoundException("Product not found"));
        return PageResponse.of(reviews.findByProductIdOrderByCreatedAtDesc(p.getId(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50))), mapper::toResponse);
    }

    public List<ReviewResponse> highlightedReviews(int limit) {
        return reviews.findHighlights(PageRequest.of(0, Math.min(limit, 20))).stream()
                .map(mapper::toResponse).toList();
    }

    public List<PromotionResponse> livePromotions() {
        var now = clock.instant();
        return promotions.findLive(now).stream().map(p -> mapper.toResponse(p, now)).toList();
    }

    public PublicSettings settings() {
        var r = props.restaurant();
        var p = props.pricing();
        return new PublicSettings(r.name(), r.tagline(), r.phone(), r.email(), r.address(), p.currency(),
                r.hours() == null ? List.of() : r.hours(), p.taxRate(), p.deliveryFee(), p.freeDeliveryThreshold(),
                p.minDeliveryOrder(), r.deliveryMinutes(), r.pickupMinutes());
    }

    static Sort sortFor(String sort) {
        if (sort == null) {
            sort = "";
        }
        return switch (sort) {
            case "popular" -> Sort.by(Sort.Order.desc("soldCount"), Sort.Order.asc("id"));
            case "price_asc" -> Sort.by(Sort.Order.asc("basePrice"), Sort.Order.asc("id"));
            case "price_desc" -> Sort.by(Sort.Order.desc("basePrice"), Sort.Order.asc("id"));
            case "rating" -> Sort.by(Sort.Order.desc("ratingAvg"), Sort.Order.desc("ratingCount"), Sort.Order.asc("id"));
            case "newest" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            case "name" -> Sort.by(Sort.Order.asc("name"));
            default -> Sort.by(Sort.Order.asc("category.sortOrder"), Sort.Order.asc("sortOrder"), Sort.Order.asc("id"));
        };
    }
}
