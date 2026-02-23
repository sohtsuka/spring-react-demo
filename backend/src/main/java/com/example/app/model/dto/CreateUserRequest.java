package com.example.app.model.dto;

import com.example.app.model.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50) String username,

        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank
        @Size(min = 8, max = 255)
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).+$",
            message = "パスワードは英大文字・英小文字・数字・記号をそれぞれ1文字以上含む必要があります"
        )
        String password,

        @NotNull UserRole role
) {}
