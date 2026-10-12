package com.crustcrunch.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** A mutually exclusive size/portion of a product (pizza size, piece count, drink size). */
@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Column(name = "is_default")
    private boolean defaultVariant;

    private boolean available = true;

    @Column(name = "sort_order")
    private int sortOrder;

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }
    public boolean isDefaultVariant() { return defaultVariant; }
    public void setDefaultVariant(boolean defaultVariant) { this.defaultVariant = defaultVariant; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
