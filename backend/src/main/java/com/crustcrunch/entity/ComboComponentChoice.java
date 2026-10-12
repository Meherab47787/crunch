package com.crustcrunch.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "combo_component_choices")
public class ComboComponentChoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id")
    private ComboComponent component;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "extra_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Column(name = "is_default")
    private boolean defaultChoice;

    public Long getId() { return id; }
    public ComboComponent getComponent() { return component; }
    public void setComponent(ComboComponent component) { this.component = component; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public BigDecimal getExtraPrice() { return extraPrice; }
    public void setExtraPrice(BigDecimal extraPrice) { this.extraPrice = extraPrice; }
    public boolean isDefaultChoice() { return defaultChoice; }
    public void setDefaultChoice(boolean defaultChoice) { this.defaultChoice = defaultChoice; }
}
