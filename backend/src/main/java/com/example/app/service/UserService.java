package com.example.app.service;

import com.example.app.model.dto.CreateUserRequest;
import com.example.app.model.dto.PagedResponse;
import com.example.app.model.dto.UpdateUserRequest;
import com.example.app.model.dto.UserResponse;

public interface UserService {

    PagedResponse<UserResponse> findAll(int page, int size);

    UserResponse findById(Long id);

    UserResponse create(CreateUserRequest request);

    UserResponse update(Long id, UpdateUserRequest request);

    void delete(Long id);

    void recordLoginFailure(String username);

    void resetLoginAttempts(String username);
}
