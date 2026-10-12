package com.crustcrunch.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/** Association between a product and a reusable customization group. */
@Entity
@Table(name = "product_customization_groups")
public class ProductCustomizationGroup {

    @EmbeddedId
    private Key id = new Key();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("productId")
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id")
    private CustomizationGroup group;

    @Column(name = "sort_order")
    private int sortOrder;

    protected ProductCustomizationGroup() {
    }

    public ProductCustomizationGroup(Product product, CustomizationGroup group, int sortOrder) {
        this.product = product;
        this.group = group;
        this.sortOrder = sortOrder;
    }

    public Product getProduct() { return product; }
    public CustomizationGroup getGroup() { return group; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "product_id")
        private Long productId;
        @Column(name = "group_id")
        private Long groupId;

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(productId, k.productId) && Objects.equals(groupId, k.groupId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId, groupId);
        }
    }
}
