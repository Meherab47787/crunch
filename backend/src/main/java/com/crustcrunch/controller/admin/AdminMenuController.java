package com.crustcrunch.controller.admin;

import com.crustcrunch.dto.AdminDtos.*;
import com.crustcrunch.dto.MenuDtos.CategoryResponse;
import com.crustcrunch.dto.MenuDtos.ProductDetail;
import com.crustcrunch.dto.MenuDtos.ProductSummary;
import com.crustcrunch.dto.PageResponse;
import com.crustcrunch.enums.ProductType;
import com.crustcrunch.service.MenuAdminService;
import com.crustcrunch.service.ProductSpecifications.ProductFilter;
import com.crustcrunch.service.StorageService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin: menu")
public class AdminMenuController {

    private final MenuAdminService menu;
    private final StorageService storage;

    public AdminMenuController(MenuAdminService menu, StorageService storage) {
        this.menu = menu;
        this.storage = storage;
    }

    // categories
    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return menu.listCategories();
    }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest req) {
        return menu.createCategory(req);
    }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return menu.updateCategory(id, req);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(@PathVariable Long id) {
        menu.deleteCategory(id);
    }

    // products
    @GetMapping("/products")
    public PageResponse<ProductSummary> products(@RequestParam(required = false) String q,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(required = false) ProductType type,
                                                 @RequestParam(required = false) Boolean available,
                                                 @RequestParam(defaultValue = "") String sort,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "50") int size) {
        ProductFilter f = new ProductFilter(q, category, null, null, null, null, available, null, null, null, null,
                type, false);
        return menu.listProducts(f, sort, page, size);
    }

    @GetMapping("/products/{id}")
    public ProductDetail product(@PathVariable Long id) {
        return menu.getProduct(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDetail createProduct(@Valid @RequestBody ProductRequest req) {
        return menu.createProduct(req);
    }

    @PutMapping("/products/{id}")
    public ProductDetail updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return menu.updateProduct(id, req);
    }

    @PatchMapping("/products/{id}/availability")
    public ProductSummary availability(@PathVariable Long id, @RequestBody AvailabilityRequest req) {
        return menu.setAvailability(id, req);
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        menu.deleteProduct(id);
    }

    // option groups
    @GetMapping("/customization-groups")
    public List<GroupAdminResponse> groups() {
        return menu.listGroups();
    }

    @PostMapping("/customization-groups")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupAdminResponse createGroup(@Valid @RequestBody GroupRequest req) {
        return menu.createGroup(req);
    }

    @PutMapping("/customization-groups/{id}")
    public GroupAdminResponse updateGroup(@PathVariable Long id, @Valid @RequestBody GroupRequest req) {
        return menu.updateGroup(id, req);
    }

    @DeleteMapping("/customization-groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Long id) {
        menu.deleteGroup(id);
    }

    // images
    @PostMapping(value = "/uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        return new UploadResponse(storage.storeImage(file));
    }
}
