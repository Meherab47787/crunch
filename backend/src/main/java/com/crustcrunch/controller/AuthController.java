package com.crustcrunch.controller;

import com.crustcrunch.dto.AuthDtos.AuthResponse;
import com.crustcrunch.dto.AuthDtos.LoginRequest;
import com.crustcrunch.dto.AuthDtos.RegisterRequest;
import com.crustcrunch.dto.UserDtos.UserResponse;
import com.crustcrunch.security.CurrentUser;
import com.crustcrunch.service.AuthService;
import com.crustcrunch.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService auth;
    private final UserService users;

    public AuthController(AuthService auth, UserService users) {
        this.auth = auth;
        this.users = users;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return users.get(CurrentUser.require().id());
    }
}
