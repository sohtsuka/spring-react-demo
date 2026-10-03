package com.example.app.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.app.model.entity.User;

public class CustomUserDetails implements UserDetails {

    private final User user;
    private final long accountId;

    public CustomUserDetails(User user) {
        this.user = user;
        this.accountId = Objects.requireNonNull(user.getId(), "Account ID is required");
    }

    public long getAccountId() {
        return accountId;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CustomUserDetails details && accountId == details.accountId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(accountId);
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        LocalDateTime lockedUntil = user.getLockedUntil();
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }
}
