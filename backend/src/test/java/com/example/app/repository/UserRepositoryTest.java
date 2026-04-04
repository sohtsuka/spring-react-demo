package com.example.app.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.example.app.model.entity.User;
import com.example.app.model.enums.UserRole;

// @MybatisTest は Spring Boot 4 で削除された FlywayAutoConfiguration を参照するため使用不可
// @SpringBootTest(webEnvironment=NONE) + Testcontainers で代替する
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    UserRepository userRepository;

    @Test
    void insert_and_findById() {
        User user = buildUser("alice", "alice@example.com");
        userRepository.insert(user);

        assertThat(user.getId()).isNotNull();
        Optional<User> found = userRepository.findById(user.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("alice");
        assertThat(found.get().getEmail()).isEqualTo("alice@example.com");
        assertThat(found.get().getRole()).isEqualTo(UserRole.USER);
    }

    @Test
    void findByUsername_returnsUser() {
        User user = buildUser("bob", "bob@example.com");
        userRepository.insert(user);

        Optional<User> found = userRepository.findByUsername("bob");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    void findByUsername_notFound_returnsEmpty() {
        Optional<User> found = userRepository.findByUsername("nonexistent");
        assertThat(found).isEmpty();
    }

    @Test
    void existsByUsername_returnsTrue() {
        User user = buildUser("charlie", "charlie@example.com");
        userRepository.insert(user);

        assertThat(userRepository.existsByUsername("charlie")).isTrue();
        assertThat(userRepository.existsByUsername("notexist")).isFalse();
    }

    @Test
    void existsByEmail_returnsTrue() {
        User user = buildUser("dave", "dave@example.com");
        userRepository.insert(user);

        assertThat(userRepository.existsByEmail("dave@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    void findAll_returnsPaginatedResults() {
        long before = userRepository.count();
        userRepository.insert(buildUser("u1", "u1@example.com"));
        userRepository.insert(buildUser("u2", "u2@example.com"));
        userRepository.insert(buildUser("u3", "u3@example.com"));

        List<User> page1 = userRepository.findAll(0, 2);
        List<User> page2 = userRepository.findAll(2, 2);

        assertThat(page1).hasSize(2);
        // 追加した3件 + 既存件数(Flyway テストデータ等)のため >=1
        assertThat(page2).isNotEmpty();
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(before + 3);
    }

    @Test
    void count_returnsCorrectCount() {
        long before = userRepository.count();
        userRepository.insert(buildUser("counter1", "counter1@example.com"));
        userRepository.insert(buildUser("counter2", "counter2@example.com"));

        assertThat(userRepository.count()).isEqualTo(before + 2);
    }

    @Test
    void update_changesFields() {
        User user = buildUser("updateme", "updateme@example.com");
        userRepository.insert(user);

        user.setUsername("updated");
        user.setRole(UserRole.MANAGER);
        userRepository.update(user);

        User found = userRepository.findById(user.getId()).orElseThrow();
        assertThat(found.getUsername()).isEqualTo("updated");
        assertThat(found.getRole()).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void deleteById_removesUser() {
        User user = buildUser("deleteme", "deleteme@example.com");
        userRepository.insert(user);

        userRepository.deleteById(user.getId());

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    void incrementFailedLoginAttempts_increments() {
        User user = buildUser("locktest", "locktest@example.com");
        userRepository.insert(user);

        userRepository.incrementFailedLoginAttempts("locktest");
        userRepository.incrementFailedLoginAttempts("locktest");

        User found = userRepository.findByUsername("locktest").orElseThrow();
        assertThat(found.getFailedLoginAttempts()).isEqualTo(2);
    }

    @Test
    void resetFailedLoginAttempts_resetsToZero() {
        User user = buildUser("resettest", "resettest@example.com");
        userRepository.insert(user);
        userRepository.incrementFailedLoginAttempts("resettest");

        userRepository.resetFailedLoginAttempts("resettest");

        User found = userRepository.findByUsername("resettest").orElseThrow();
        assertThat(found.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(found.getLockedUntil()).isNull();
    }

    private User buildUser(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("$argon2id$v=19$m=65536,t=3,p=1$dummy$dummy");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        return user;
    }
}
