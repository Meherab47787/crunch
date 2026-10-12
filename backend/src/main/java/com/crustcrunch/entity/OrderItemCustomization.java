package com.crustcrunch.entity;

import com.crustcrunch.enums.CustomizationKind;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_item_customizations")
public class OrderItemCustomization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CustomizationKind kind;

    @Column(name = "group_name", nullable = false)
    private String groupName;

    @Column(name = "option_name", nullable = false)
    private String optionName;

    @Column(name = "price_delta", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "choice_product_id")
    private Long choiceProductId;

    public Long getId() { return id; }
    public OrderItem getOrderItem() { return orderItem; }
    public void setOrderItem(OrderItem orderItem) { this.orderItem = orderItem; }
    public CustomizationKind getKind() { return kind; }
    public void setKind(CustomizationKind kind) { this.kind = kind; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getOptionName() { return optionName; }
    public void setOptionName(String optionName) { this.optionName = optionName; }
    public BigDecimal getPriceDelta() { return priceDelta; }
    public void setPriceDelta(BigDecimal priceDelta) { this.priceDelta = priceDelta; }
    public Long getOptionId() { return optionId; }
    public void setOptionId(Long optionId) { this.optionId = optionId; }
    public Long getChoiceProductId() { return choiceProductId; }
    public void setChoiceProductId(Long choiceProductId) { this.choiceProductId = choiceProductId; }
}
