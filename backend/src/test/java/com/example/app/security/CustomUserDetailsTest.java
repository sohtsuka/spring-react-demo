package com.example.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.example.app.model.entity.User;
import com.example.app.model.enums.UserRole;

class CustomUserDetailsTest {

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashed");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        return user;
    }

    @Test
    void isAccountNonLocked_whenLockedUntilIsNull_returnsTrue() {
        User user = buildUser();
        user.setLockedUntil(null);
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void isAccountNonLocked_whenLockedUntilIsInPast_returnsTrue() {
        User user = buildUser();
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isAccountNonLocked()).isTrue();
    }

    @Test
    void isAccountNonLocked_whenLockedUntilIsInFuture_returnsFalse() {
        User user = buildUser();
        user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
        CustomUserDetails details = new CustomUserDetails(user);

        assertThat(details.isAccountNonLocked()).isFalse();
    }
}
