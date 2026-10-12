package com.crustcrunch.entity;

import com.crustcrunch.enums.SelectionType;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/** A reusable set of options (e.g. "Crust", "Sauces") that can be attached to many products. */
@Entity
@Table(name = "customization_groups")
public class CustomizationGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_type", nullable = false)
    private SelectionType selectionType;

    @Column(name = "min_select", nullable = false)
    private int minSelect;

    @Column(name = "max_select", nullable = false)
    private int maxSelect = 1;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<CustomizationOption> options = new ArrayList<>();

    public boolean isRequired() {
        return minSelect > 0;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public SelectionType getSelectionType() { return selectionType; }
    public void setSelectionType(SelectionType selectionType) { this.selectionType = selectionType; }
    public int getMinSelect() { return minSelect; }
    public void setMinSelect(int minSelect) { this.minSelect = minSelect; }
    public int getMaxSelect() { return maxSelect; }
    public void setMaxSelect(int maxSelect) { this.maxSelect = maxSelect; }
    public List<CustomizationOption> getOptions() { return options; }
}
