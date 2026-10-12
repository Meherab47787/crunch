package com.crustcrunch.service;

import com.crustcrunch.dto.MenuDtos.ReviewResponse;
import com.crustcrunch.dto.OrderDtos.CreateReviewRequest;
import com.crustcrunch.entity.Product;
import com.crustcrunch.entity.Review;
import com.crustcrunch.exception.BusinessRuleException;
import com.crustcrunch.exception.ConflictException;
import com.crustcrunch.exception.NotFoundException;
import com.crustcrunch.mapper.MenuMapper;
import com.crustcrunch.repository.OrderRepository;
import com.crustcrunch.repository.ProductRepository;
import com.crustcrunch.repository.ReviewRepository;
import com.crustcrunch.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ReviewService {

    private final ReviewRepository reviews;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;
    private final MenuMapper mapper;

    public ReviewService(ReviewRepository reviews, OrderRepository orders, ProductRepository products,
                         UserRepository users, MenuMapper mapper) {
        this.reviews = reviews;
        this.orders = orders;
        this.products = products;
        this.users = users;
        this.mapper = mapper;
    }

    /** Customers may review a product once per delivered order that contained it. */
    @Transactional
    public ReviewResponse create(Long userId, CreateReviewRequest req) {
        if (!orders.isReviewEligible(req.orderId(), userId, req.productId())) {
            throw new BusinessRuleException("You can only review items from your delivered orders");
        }
        if (reviews.existsByUserIdAndProductIdAndOrderId(userId, req.productId(), req.orderId())) {
            throw new ConflictException("You've already reviewed this item for this order");
        }
        Product p = products.findById(req.productId()).orElseThrow(() -> new NotFoundException("Product not found"));
        Review r = new Review();
        r.setUser(users.getReferenceById(userId));
        r.setProduct(p);
        r.setOrder(orders.getReferenceById(req.orderId()));
        r.setRating(req.rating());
        r.setComment(AuthService.blankToNull(req.comment()));
        reviews.save(r);

        // Incremental running average keeps listing queries cheap.
        int count = p.getRatingCount();
        BigDecimal total = p.getRatingAvg().multiply(BigDecimal.valueOf(count)).add(BigDecimal.valueOf(req.rating()));
        p.setRatingCount(count + 1);
        p.setRatingAvg(total.divide(BigDecimal.valueOf(count + 1), 2, RoundingMode.HALF_UP));
        return mapper.toResponse(reviews.findById(r.getId()).orElseThrow());
    }
}
