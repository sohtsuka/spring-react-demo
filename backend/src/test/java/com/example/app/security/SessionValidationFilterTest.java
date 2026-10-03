package com.example.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.app.model.entity.User;
import com.example.app.model.enums.UserRole;
import com.example.app.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SessionValidationFilterTest {
    @Mock
    UserRepository users;

    @Mock
    FilterChain chain;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unauthenticatedRequestContinuesWithoutDatabaseLookup() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        new SessionValidationFilter(users).doFilter(request, response, chain);

        then(chain).should().doFilter(request, response);
        then(users).shouldHaveNoInteractions();
    }

    @Test
    void otherPrincipalContinuesWithoutDatabaseLookup() throws Exception {
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated("other", null, List.of()));
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        new SessionValidationFilter(users).doFilter(request, response, chain);

        then(chain).should().doFilter(request, response);
        then(users).shouldHaveNoInteractions();
    }

    @Test
    void currentOrPreviouslyExpiredLockKeepsSession() throws Exception {
        for (boolean expiredLock : List.of(false, true)) {
            authenticate(user());
            User current = user();
            if (expiredLock) {
                current.setLockedUntil(LocalDateTime.now().minusMinutes(1));
            }
            given(users.findById(1L)).willReturn(Optional.of(current));
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();
            var session = request.getSession();

            new SessionValidationFilter(users).doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(request.getSession(false)).isSameAs(session);
            then(chain).should().doFilter(request, response);
        }
    }

    @Test
    void changedAccountInvalidatesSessionAndClearsAuthentication() throws Exception {
        List<Consumer<User>> changes = List.of(current -> current.setEnabled(false),
                current -> current.setLockedUntil(LocalDateTime.now().plusMinutes(30)),
                current -> current.setRole(UserRole.USER), current -> current.setUsername("renamed"),
                current -> current.setPassword("new-hash"));
        for (Consumer<User> change : changes) {
            authenticate(user());
            User current = user();
            change.accept(current);
            given(users.findById(1L)).willReturn(Optional.of(current));
            var request = new MockHttpServletRequest();
            var response = new MockHttpServletResponse();
            request.getSession();

            new SessionValidationFilter(users).doFilter(request, response, chain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(request.getSession(false)).isNull();
            then(chain).should().doFilter(request, response);
        }
    }

    @Test
    void deletedAccountWithoutSessionClearsAuthentication() throws Exception {
        authenticate(user());
        given(users.findById(1L)).willReturn(Optional.empty());
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        new SessionValidationFilter(users).doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getSession(false)).isNull();
        then(chain).should().doFilter(request, response);
    }

    @Test
    void databaseFailureReturnsUtf8ErrorWithoutContinuing() throws Exception {
        authenticate(user());
        given(users.findById(1L)).willThrow(new DataAccessResourceFailureException("offline"));
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        new SessionValidationFilter(users).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("INTERNAL_SERVER_ERROR", "サーバー内部エラーが発生しました");
        then(chain).should(never()).doFilter(request, response);
    }

    private void authenticate(User user) {
        var details = new CustomUserDetails(user);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities()));
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("hash");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        return user;
    }
}
