package com.example.app.model.dto;

import java.time.LocalDateTime;

import com.example.app.model.entity.User;
import com.example.app.model.enums.UserRole;

public record UserResponse(Long id, String username, String email, UserRole role, boolean enabled,
        LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isEnabled(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
