package com.crustcrunch.controller.admin;

import com.crustcrunch.dto.OrderDtos.OrderSummaryResponse;
import com.crustcrunch.dto.PageResponse;
import com.crustcrunch.dto.UserDtos.*;
import com.crustcrunch.security.CurrentUser;
import com.crustcrunch.service.AdminUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin: users & staff")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @GetMapping("/customers")
    public PageResponse<CustomerResponse> customers(@RequestParam(required = false) String q,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.customers(q, page, size);
    }

    @GetMapping("/customers/{id}")
    public CustomerResponse customer(@PathVariable Long id) {
        return service.customer(id);
    }

    @GetMapping("/customers/{id}/orders")
    public PageResponse<OrderSummaryResponse> customerOrders(@PathVariable Long id,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return service.customerOrders(id, page, size);
    }

    @GetMapping("/staff")
    public PageResponse<UserResponse> staff(@RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "50") int size) {
        return service.staff(q, page, size);
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createStaff(@Valid @RequestBody StaffCreateRequest req) {
        return service.createStaff(req);
    }

    @PutMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody UserAdminUpdateRequest req) {
        return service.update(id, CurrentUser.require().id(), req);
    }
}
