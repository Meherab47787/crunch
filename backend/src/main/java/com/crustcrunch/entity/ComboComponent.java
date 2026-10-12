package com.crustcrunch.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** A slot within a combo ("Choose a side"), filled {@code quantity} times from its eligible choices. */
@Entity
@Table(name = "combo_components")
public class ComboComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "combo_product_id")
    private Product comboProduct;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(name = "sort_order")
    private int sortOrder;

    @OneToMany(mappedBy = "component", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ComboComponentChoice> choices = new ArrayList<>();

    public Long getId() { return id; }
    public Product getComboProduct() { return comboProduct; }
    public void setComboProduct(Product comboProduct) { this.comboProduct = comboProduct; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public List<ComboComponentChoice> getChoices() { return choices; }
}
