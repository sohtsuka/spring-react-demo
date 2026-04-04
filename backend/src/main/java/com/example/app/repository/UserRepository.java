package com.example.app.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.app.model.entity.User;

@Mapper
public interface UserRepository {

    Optional<User> findById(@Param("id") Long id);

    Optional<User> findByUsername(@Param("username") String username);

    Optional<User> findByEmail(@Param("email") String email);

    List<User> findAll(@Param("offset") int offset, @Param("limit") int limit);

    long count();

    boolean existsByUsername(@Param("username") String username);

    boolean existsByEmail(@Param("email") String email);

    void insert(User user);

    void update(User user);

    void deleteById(@Param("id") Long id);

    void incrementFailedLoginAttempts(@Param("username") String username);

    void lockAccount(@Param("username") String username, @Param("lockedUntil") LocalDateTime lockedUntil);

    void resetFailedLoginAttempts(@Param("username") String username);
}
