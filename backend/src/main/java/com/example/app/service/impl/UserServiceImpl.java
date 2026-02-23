package com.example.app.service.impl;

import com.example.app.exception.AppException;
import com.example.app.exception.ErrorCode;
import com.example.app.model.dto.CreateUserRequest;
import com.example.app.model.dto.PagedResponse;
import com.example.app.model.dto.UpdateUserRequest;
import com.example.app.model.dto.UserResponse;
import com.example.app.model.entity.User;
import com.example.app.repository.UserRepository;
import com.example.app.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final int MAX_FAILED_ATTEMPTS = 10;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> findAll(int page, int size) {
        int offset = (page - 1) * size;
        List<UserResponse> users = userRepository.findAll(offset, size)
                .stream()
                .map(UserResponse::from)
                .toList();
        long total = userRepository.count();
        return PagedResponse.of(users, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }

    @Override
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        userRepository.insert(user);

        // INSERT は created_at/updated_at を DB 側で設定するため再取得する
        User created = userRepository.findById(user.getId()).orElseThrow();
        return UserResponse.from(created);
    }

    @Override
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.username() != null && !request.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.username())) {
                throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
            }
            user.setUsername(request.username());
        }
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.setEmail(request.email());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        userRepository.update(user);
        // UPDATE は updated_at を DB 側で設定するため再取得する
        User updated = userRepository.findById(user.getId()).orElseThrow();
        return UserResponse.from(updated);
    }

    @Override
    public void delete(Long id) {
        if (userRepository.findById(id).isEmpty()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        userRepository.deleteById(id);
    }

    @Override
    public void recordLoginFailure(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            int newAttempts = user.getFailedLoginAttempts() + 1;
            userRepository.incrementFailedLoginAttempts(username);
            if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                LocalDateTime lockedUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
                userRepository.lockAccount(username, lockedUntil);
            }
        });
    }

    @Override
    public void resetLoginAttempts(String username) {
        userRepository.resetFailedLoginAttempts(username);
    }
}
