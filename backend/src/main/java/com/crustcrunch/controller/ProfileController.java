package com.crustcrunch.controller;

import com.crustcrunch.dto.UserDtos.*;
import com.crustcrunch.security.CurrentUser;
import com.crustcrunch.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@Tag(name = "Customer profile")
public class ProfileController {

    private final UserService users;

    public ProfileController(UserService users) {
        this.users = users;
    }

    @GetMapping
    public UserResponse profile() {
        return users.get(CurrentUser.require().id());
    }

    @PutMapping
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest req) {
        return users.updateProfile(CurrentUser.require().id(), req);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        users.changePassword(CurrentUser.require().id(), req);
    }

    @GetMapping("/addresses")
    public List<AddressResponse> addresses() {
        return users.listAddresses(CurrentUser.require().id());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse createAddress(@Valid @RequestBody AddressRequest req) {
        return users.createAddress(CurrentUser.require().id(), req);
    }

    @PutMapping("/addresses/{id}")
    public AddressResponse updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest req) {
        return users.updateAddress(CurrentUser.require().id(), id, req);
    }

    @DeleteMapping("/addresses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAddress(@PathVariable Long id) {
        users.deleteAddress(CurrentUser.require().id(), id);
    }
}
