package com.crustcrunch.security;

import com.crustcrunch.entity.User;
import com.crustcrunch.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Authenticated principal stored in the security context. */
public record AuthUser(Long id, String email, String passwordHash, Role role, boolean enabled) implements UserDetails {

    public static AuthUser from(User u) {
        return new AuthUser(u.getId(), u.getEmail(), u.getPasswordHash(), u.getRole(), u.isEnabled());
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isStaff() {
        return role == Role.ADMIN || role == Role.KITCHEN_STAFF;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
