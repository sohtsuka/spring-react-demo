package com.example.app.service;

import com.example.app.exception.AppException;
import com.example.app.exception.ErrorCode;
import com.example.app.model.dto.CreateUserRequest;
import com.example.app.model.dto.PagedResponse;
import com.example.app.model.dto.UpdateUserRequest;
import com.example.app.model.dto.UserResponse;
import com.example.app.model.entity.User;
import com.example.app.model.enums.UserRole;
import com.example.app.repository.UserRepository;
import com.example.app.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    void findAll_returnsPagedResponse() {
        given(userRepository.findAll(0, 20)).willReturn(List.of(buildUser()));
        given(userRepository.count()).willReturn(1L);

        PagedResponse<UserResponse> result = userService.findAll(1, 20);

        assertThat(result.data()).hasSize(1);
        assertThat(result.pagination().totalElements()).isEqualTo(1);
        assertThat(result.pagination().totalPages()).isEqualTo(1);
    }

    @Test
    void findById_existingUser_returnsUserResponse() {
        given(userRepository.findById(1L)).willReturn(Optional.of(buildUser()));

        UserResponse result = userService.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.username()).isEqualTo("testuser");
    }

    @Test
    void findById_notFound_throwsAppException() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void create_newUser_insertsAndReturns() {
        CreateUserRequest request = new CreateUserRequest(
                "newuser", "new@example.com", "Password1!", UserRole.USER);
        given(userRepository.existsByUsername("newuser")).willReturn(false);
        given(userRepository.existsByEmail("new@example.com")).willReturn(false);
        given(passwordEncoder.encode("Password1!")).willReturn("hashedPassword");
        // INSERT は DB 側で id を生成するため、insert 後の findById をモックする
        User created = buildUser();
        created.setUsername("newuser");
        created.setEmail("new@example.com");
        created.setPassword("hashedPassword");
        // insert モックは id を設定しないので user.getId() == null → any() で null も含めてマッチ
        given(userRepository.findById(any())).willReturn(Optional.of(created));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        UserResponse result = userService.create(request);

        then(userRepository).should().insert(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("newuser");
        assertThat(captor.getValue().getPassword()).isEqualTo("hashedPassword");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(result.username()).isEqualTo("newuser");
    }

    @Test
    void create_duplicateUsername_throwsAppException() {
        given(userRepository.existsByUsername("existing")).willReturn(true);

        assertThatThrownBy(() -> userService.create(
                new CreateUserRequest("existing", "e@example.com", "Password1!", UserRole.USER)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS));
    }

    @Test
    void create_duplicateEmail_throwsAppException() {
        given(userRepository.existsByUsername(anyString())).willReturn(false);
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> userService.create(
                new CreateUserRequest("newuser", "dup@example.com", "Password1!", UserRole.USER)))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
    }

    @Test
    void update_existingUser_updatesAndReturns() {
        User user = buildUser();
        // findById は update 前(既存取得)と update 後(再取得)の2回呼ばれる
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        UpdateUserRequest request = new UpdateUserRequest(null, null, UserRole.MANAGER, null);
        UserResponse result = userService.update(1L, request);

        // user オブジェクトは update 前に MANAGER に変更されるため、再取得も同じ値を返す
        assertThat(result.role()).isEqualTo(UserRole.MANAGER);
        then(userRepository).should().update(user);
    }

    @Test
    void delete_existingUser_callsDeleteById() {
        given(userRepository.findById(1L)).willReturn(Optional.of(buildUser()));

        userService.delete(1L);

        then(userRepository).should().deleteById(1L);
    }

    @Test
    void delete_notFound_throwsAppException() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.delete(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void recordLoginFailure_incrementsAttempts() {
        User user = buildUser();
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));

        userService.recordLoginFailure("testuser");

        then(userRepository).should().incrementFailedLoginAttempts("testuser");
    }

    @Test
    void recordLoginFailure_reachesMaxAttempts_locksAccount() {
        User user = buildUser();
        user.setFailedLoginAttempts(9);
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(user));

        userService.recordLoginFailure("testuser");

        then(userRepository).should().incrementFailedLoginAttempts("testuser");
        then(userRepository).should().lockAccount(anyString(), any(LocalDateTime.class));
    }

    @Test
    void resetLoginAttempts_callsReset() {
        userService.resetLoginAttempts("testuser");
        then(userRepository).should().resetFailedLoginAttempts("testuser");
    }

    private User buildUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("hashed");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
