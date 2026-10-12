package com.crustcrunch.service;

import com.crustcrunch.dto.AuthDtos.AuthResponse;
import com.crustcrunch.dto.AuthDtos.LoginRequest;
import com.crustcrunch.dto.AuthDtos.RegisterRequest;
import com.crustcrunch.entity.User;
import com.crustcrunch.enums.Role;
import com.crustcrunch.exception.ConflictException;
import com.crustcrunch.mapper.UserMapper;
import com.crustcrunch.repository.UserRepository;
import com.crustcrunch.security.AuthUser;
import com.crustcrunch.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtService jwt;
    private final UserMapper mapper;

    public AuthService(UserRepository users, PasswordEncoder encoder, AuthenticationManager authManager,
                       JwtService jwt, UserMapper mapper) {
        this.users = users;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwt = jwt;
        this.mapper = mapper;
    }

    /** Public registration always creates a CUSTOMER; staff accounts are created by admins. */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(req.password()));
        u.setFullName(req.fullName().trim());
        u.setPhone(blankToNull(req.phone()));
        u.setRole(Role.CUSTOMER);
        users.save(u);
        return new AuthResponse(jwt.issue(AuthUser.from(u)), jwt.getExpirationSeconds(), mapper.toResponse(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        AuthUser principal = (AuthUser) auth.getPrincipal();
        User u = users.findById(principal.id()).orElseThrow();
        return new AuthResponse(jwt.issue(principal), jwt.getExpirationSeconds(), mapper.toResponse(u));
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
