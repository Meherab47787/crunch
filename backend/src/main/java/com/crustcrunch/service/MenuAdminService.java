package com.crustcrunch.service;

import com.crustcrunch.dto.AdminDtos.*;
import com.crustcrunch.dto.MenuDtos.CategoryResponse;
import com.crustcrunch.dto.MenuDtos.ProductDetail;
import com.crustcrunch.dto.MenuDtos.ProductSummary;
import com.crustcrunch.dto.PageResponse;
import com.crustcrunch.entity.*;
import com.crustcrunch.enums.ProductType;
import com.crustcrunch.enums.SelectionType;
import com.crustcrunch.exception.BadRequestException;
import com.crustcrunch.exception.BusinessRuleException;
import com.crustcrunch.exception.ConflictException;
import com.crustcrunch.exception.NotFoundException;
import com.crustcrunch.mapper.MenuMapper;
import com.crustcrunch.repository.CategoryRepository;
import com.crustcrunch.repository.CustomizationGroupRepository;
import com.crustcrunch.repository.ProductRepository;
import com.crustcrunch.service.ProductSpecifications.ProductFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Admin CRUD for categories, products (incl. variants, option links, combos) and option groups. */
@Service
@Transactional
public class MenuAdminService {

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final CustomizationGroupRepository groups;
    private final PricingService pricing;
    private final MenuMapper mapper;

    public MenuAdminService(CategoryRepository categories, ProductRepository products,
                            CustomizationGroupRepository groups, PricingService pricing, MenuMapper mapper) {
        this.categories = categories;
        this.products = products;
        this.groups = groups;
        this.pricing = pricing;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------- categories

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        Map<Long, Long> counts = products.countAllByCategory().stream()
                .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
        return categories.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(c -> mapper.toResponse(c, counts.getOrDefault(c.getId(), 0L))).toList();
    }

    public CategoryResponse createCategory(CategoryRequest req) {
        Category c = new Category();
        applyCategory(c, req);
        if (categories.existsBySlug(c.getSlug())) {
            throw new ConflictException("A category with slug '" + c.getSlug() + "' already exists");
        }
        return mapper.toResponse(categories.save(c), 0);
    }

    public CategoryResponse updateCategory(Long id, CategoryRequest req) {
        Category c = categories.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        applyCategory(c, req);
        if (categories.existsBySlugAndIdNot(c.getSlug(), id)) {
            throw new ConflictException("A category with slug '" + c.getSlug() + "' already exists");
        }
        return mapper.toResponse(c, products.countByCategoryId(id));
    }

    public void deleteCategory(Long id) {
        Category c = categories.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
        if (products.countByCategoryId(id) > 0) {
            throw new BusinessRuleException("Move or delete this category's products first, or deactivate it instead");
        }
        categories.delete(c);
    }

    private void applyCategory(Category c, CategoryRequest req) {
        c.setName(req.name().trim());
        c.setSlug(req.slug() == null || req.slug().isBlank() ? SlugUtil.slugify(req.name()) : req.slug());
        c.setDescription(AuthService.blankToNull(req.description()));
        c.setImageUrl(AuthService.blankToNull(req.imageUrl()));
        c.setIcon(AuthService.blankToNull(req.icon()));
        c.setSortOrder(req.sortOrder());
        c.setActive(req.active());
    }

    // ------------------------------------------------------------- products

    @Transactional(readOnly = true)
    public PageResponse<ProductSummary> listProducts(ProductFilter filter, String sort, int page, int size) {
        var result = products.findAll(ProductSpecifications.of(filter),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), CatalogService.sortFor(sort)));
        var live = pricing.livePromotions();
        return PageResponse.of(result, p -> mapper.toSummary(p, live));
    }

    @Transactional(readOnly = true)
    public ProductDetail getProduct(Long id) {
        return mapper.toDetail(loadProduct(id), pricing.livePromotions());
    }

    public ProductDetail createProduct(ProductRequest req) {
        Product p = new Product();
        applyProduct(p, req);
        if (products.existsBySlug(p.getSlug())) {
            throw new ConflictException("A product with slug '" + p.getSlug() + "' already exists");
        }
        products.save(p);
        applyChildren(p, req);
        return mapper.toDetail(products.saveAndFlush(p), pricing.livePromotions());
    }

    public ProductDetail updateProduct(Long id, ProductRequest req) {
        Product p = loadProduct(id);
        applyProduct(p, req);
        if (products.existsBySlugAndIdNot(p.getSlug(), id)) {
            throw new ConflictException("A product with slug '" + p.getSlug() + "' already exists");
        }
        applyChildren(p, req);
        return mapper.toDetail(products.saveAndFlush(p), pricing.livePromotions());
    }

    public ProductSummary setAvailability(Long id, AvailabilityRequest req) {
        Product p = loadProduct(id);
        if (req.available() != null) {
            p.setAvailable(req.available());
        }
        if (req.visible() != null) {
            p.setVisible(req.visible());
        }
        return mapper.toSummary(p, pricing.livePromotions());
    }

    /** Past orders keep their snapshot; the FK on order_items is set to NULL by the database. */
    public void deleteProduct(Long id) {
        products.delete(loadProduct(id));
    }

    private void applyProduct(Product p, ProductRequest req) {
        Category c = categories.findById(req.categoryId()).orElseThrow(() -> new BadRequestException("Unknown category"));
        p.setCategory(c);
        p.setName(req.name().trim());
        p.setSlug(req.slug() == null || req.slug().isBlank() ? SlugUtil.slugify(req.name()) : req.slug());
        p.setDescription(AuthService.blankToNull(req.description()));
        p.setIngredients(AuthService.blankToNull(req.ingredients()));
        p.setAllergens(AuthService.blankToNull(req.allergens()));
        p.setBasePrice(PricingService.money(req.basePrice()));
        p.setImageUrl(AuthService.blankToNull(req.imageUrl()));
        p.setProductType(req.productType());
        p.setVegetarian(req.vegetarian());
        p.setSpicy(req.spicy());
        p.setCalories(req.calories());
        p.setPrepMinutes(req.prepMinutes());
        p.setAvailable(req.available());
        p.setVisible(req.visible());
        p.setFeatured(req.featured());
        p.setBestseller(req.bestseller());
        p.setNewArrival(req.newArrival());
        p.setSortOrder(req.sortOrder());
    }

    /**
     * Children are merged by id rather than replaced, so variant/option ids saved in customers'
     * carts stay valid across edits.
     */
    private void applyChildren(Product p, ProductRequest req) {
        // variants
        List<VariantRequest> vreqs = Optional.ofNullable(req.variants()).orElse(List.of());
        if (vreqs.stream().filter(VariantRequest::defaultVariant).count() > 1) {
            throw new BadRequestException("Only one size can be the default");
        }
        Map<Long, ProductVariant> existingVariants = p.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        List<ProductVariant> nextVariants = new ArrayList<>();
        for (VariantRequest vr : vreqs) {
            ProductVariant v = vr.id() != null ? existingVariants.get(vr.id()) : null;
            if (vr.id() != null && v == null) {
                throw new BadRequestException("Variant " + vr.id() + " does not belong to this product");
            }
            if (v == null) {
                v = new ProductVariant();
                v.setProduct(p);
            }
            v.setName(vr.name().trim());
            v.setPriceDelta(PricingService.money(vr.priceDelta()));
            v.setDefaultVariant(vr.defaultVariant());
            v.setAvailable(vr.available());
            v.setSortOrder(vr.sortOrder());
            nextVariants.add(v);
        }
        if (!nextVariants.isEmpty() && nextVariants.stream().noneMatch(ProductVariant::isDefaultVariant)) {
            nextVariants.getFirst().setDefaultVariant(true);
        }
        p.getVariants().clear();
        p.getVariants().addAll(nextVariants);

        // option groups (ordered)
        List<Long> groupIds = Optional.ofNullable(req.customizationGroupIds()).orElse(List.of());
        if (new HashSet<>(groupIds).size() != groupIds.size()) {
            throw new BadRequestException("An option group is listed twice");
        }
        Map<Long, CustomizationGroup> found = groups.findAllById(groupIds).stream()
                .collect(Collectors.toMap(CustomizationGroup::getId, Function.identity()));
        if (found.size() != groupIds.size()) {
            throw new BadRequestException("Unknown option group");
        }
        Map<Long, ProductCustomizationGroup> existingLinks = p.getCustomizationGroups().stream()
                .collect(Collectors.toMap(l -> l.getGroup().getId(), Function.identity()));
        List<ProductCustomizationGroup> nextLinks = new ArrayList<>();
        for (int i = 0; i < groupIds.size(); i++) {
            ProductCustomizationGroup link = existingLinks.get(groupIds.get(i));
            if (link == null) {
                link = new ProductCustomizationGroup(p, found.get(groupIds.get(i)), i + 1);
            } else {
                link.setSortOrder(i + 1);
            }
            nextLinks.add(link);
        }
        p.getCustomizationGroups().clear();
        p.getCustomizationGroups().addAll(nextLinks);

        // combo components
        List<ComboComponentRequest> creqs = Optional.ofNullable(req.comboComponents()).orElse(List.of());
        if (p.getProductType() == ProductType.COMBO && creqs.isEmpty()) {
            throw new BusinessRuleException("A combo needs at least one component");
        }
        if (p.getProductType() == ProductType.STANDARD && !creqs.isEmpty()) {
            throw new BusinessRuleException("Only combo products can have combo components");
        }
        Set<Long> choiceProductIds = creqs.stream().flatMap(c -> c.choices().stream())
                .map(ComboChoiceRequest::productId).collect(Collectors.toSet());
        Map<Long, Product> choiceProducts = products.findAllById(choiceProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, ComboComponent> existingComps = p.getComboComponents().stream()
                .collect(Collectors.toMap(ComboComponent::getId, Function.identity()));
        List<ComboComponent> nextComps = new ArrayList<>();
        for (ComboComponentRequest cr : creqs) {
            ComboComponent comp = cr.id() != null ? existingComps.get(cr.id()) : null;
            if (cr.id() != null && comp == null) {
                throw new BadRequestException("Combo component " + cr.id() + " does not belong to this product");
            }
            if (comp == null) {
                comp = new ComboComponent();
                comp.setComboProduct(p);
            }
            comp.setName(cr.name().trim());
            comp.setQuantity(cr.quantity());
            comp.setSortOrder(cr.sortOrder());
            Map<Long, ComboComponentChoice> existingChoices = comp.getChoices().stream()
                    .collect(Collectors.toMap(ch -> ch.getProduct().getId(), Function.identity()));
            List<ComboComponentChoice> nextChoices = new ArrayList<>();
            Set<Long> seen = new HashSet<>();
            for (ComboChoiceRequest chr : cr.choices()) {
                Product choiceProduct = choiceProducts.get(chr.productId());
                if (choiceProduct == null) {
                    throw new BadRequestException("Unknown product in combo choices: " + chr.productId());
                }
                if (choiceProduct.getProductType() == ProductType.COMBO) {
                    throw new BusinessRuleException("Combos cannot contain other combos");
                }
                if (!seen.add(chr.productId())) {
                    throw new BadRequestException(choiceProduct.getName() + " is listed twice in '" + cr.name() + "'");
                }
                ComboComponentChoice ch = existingChoices.get(chr.productId());
                if (ch == null) {
                    ch = new ComboComponentChoice();
                    ch.setComponent(comp);
                    ch.setProduct(choiceProduct);
                }
                ch.setExtraPrice(PricingService.money(chr.extraPrice()));
                ch.setDefaultChoice(chr.defaultChoice());
                nextChoices.add(ch);
            }
            comp.getChoices().clear();
            comp.getChoices().addAll(nextChoices);
            nextComps.add(comp);
        }
        p.getComboComponents().clear();
        p.getComboComponents().addAll(nextComps);

        // gallery
        if (req.galleryImages() != null) {
            p.getImages().clear();
            int i = 0;
            for (String url : req.galleryImages()) {
                ProductImage img = new ProductImage();
                img.setProduct(p);
                img.setUrl(url.trim());
                img.setAltText(p.getName());
                img.setSortOrder(i++);
                p.getImages().add(img);
            }
        }
    }

    private Product loadProduct(Long id) {
        return products.findById(id).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    // ------------------------------------------------------------- option groups

    @Transactional(readOnly = true)
    public List<GroupAdminResponse> listGroups() {
        return groups.findAllByOrderByNameAsc().stream()
                .map(g -> new GroupAdminResponse(mapper.toResponse(g), groups.countProductUsages(g.getId())))
                .toList();
    }

    public GroupAdminResponse createGroup(GroupRequest req) {
        CustomizationGroup g = new CustomizationGroup();
        applyGroup(g, req);
        return new GroupAdminResponse(mapper.toResponse(groups.saveAndFlush(g)), 0);
    }

    public GroupAdminResponse updateGroup(Long id, GroupRequest req) {
        CustomizationGroup g = groups.findById(id).orElseThrow(() -> new NotFoundException("Option group not found"));
        applyGroup(g, req);
        return new GroupAdminResponse(mapper.toResponse(groups.saveAndFlush(g)), groups.countProductUsages(id));
    }

    public void deleteGroup(Long id) {
        CustomizationGroup g = groups.findById(id).orElseThrow(() -> new NotFoundException("Option group not found"));
        long usage = groups.countProductUsages(id);
        if (usage > 0) {
            throw new BusinessRuleException("This option group is used by " + usage + " product(s). Detach it first.");
        }
        groups.delete(g);
    }

    private void applyGroup(CustomizationGroup g, GroupRequest req) {
        int max = req.selectionType() == SelectionType.SINGLE ? 1 : req.maxSelect();
        int min = Math.min(req.minSelect(), max);
        if (req.minSelect() > max) {
            throw new BadRequestException("Minimum selections cannot exceed maximum selections");
        }
        if (min > req.options().size()) {
            throw new BadRequestException("Not enough options to satisfy the minimum selection");
        }
        g.setName(req.name().trim());
        g.setDescription(AuthService.blankToNull(req.description()));
        g.setSelectionType(req.selectionType());
        g.setMinSelect(min);
        g.setMaxSelect(max);
        Map<Long, CustomizationOption> existing = g.getOptions().stream()
                .collect(Collectors.toMap(CustomizationOption::getId, Function.identity()));
        List<CustomizationOption> next = new ArrayList<>();
        for (OptionRequest or : req.options()) {
            CustomizationOption o = or.id() != null ? existing.get(or.id()) : null;
            if (or.id() != null && o == null) {
                throw new BadRequestException("Option " + or.id() + " does not belong to this group");
            }
            if (o == null) {
                o = new CustomizationOption();
                o.setGroup(g);
            }
            o.setName(or.name().trim());
            o.setPriceDelta(PricingService.money(or.priceDelta()));
            o.setDefaultOption(or.defaultOption());
            o.setAvailable(or.available());
            o.setSortOrder(or.sortOrder());
            next.add(o);
        }
        g.getOptions().clear();
        g.getOptions().addAll(next);
    }
}
