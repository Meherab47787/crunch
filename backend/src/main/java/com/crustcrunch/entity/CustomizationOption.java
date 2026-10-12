package com.crustcrunch.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "customization_options")
public class CustomizationOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private CustomizationGroup group;

    @Column(nullable = false)
    private String name;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Column(name = "is_default")
    private boolean defaultOption;

    private boolean available = true;

    @Column(name = "sort_order")
    private int sortOrder;

    public Long getId() { return id; }
    public CustomizationGroup getGroup() { return group; }
    public void setGroup(CustomizationGroup group) { this.group = group; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }
    public boolean isDefaultOption() { return defaultOption; }
    public void setDefaultOption(boolean defaultOption) { this.defaultOption = defaultOption; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
