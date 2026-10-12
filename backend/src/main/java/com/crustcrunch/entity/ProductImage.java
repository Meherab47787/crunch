package com.crustcrunch.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private String url;

    @Column(name = "alt_text")
    private String altText;

    @Column(name = "sort_order")
    private int sortOrder;

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getAltText() { return altText; }
    public void setAltText(String altText) { this.altText = altText; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
