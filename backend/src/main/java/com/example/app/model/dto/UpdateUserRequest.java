package com.example.app.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import com.example.app.model.enums.UserRole;

public record UpdateUserRequest(@Size(min = 3, max = 50) String username, @Email @Size(max = 255) String email,
        UserRole role, Boolean enabled) {
}
