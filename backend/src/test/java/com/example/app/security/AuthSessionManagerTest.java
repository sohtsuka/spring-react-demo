package com.example.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionDestroyedEvent;

import com.example.app.model.entity.User;
import com.example.app.model.enums.UserRole;

class AuthSessionManagerTest {
    private final SessionRegistryImpl registry = new SessionRegistryImpl();
    private final AuthSessionManager manager = manager();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rotatesExistingSessionAndCsrfWithoutMigratingAttributes() {
        var request = new MockHttpServletRequest();
        var old = request.getSession();
        old.setAttribute("untrusted", "value");
        var response = new MockHttpServletResponse();
        login(1, request, response);
        assertThat(request.getSession().getId()).isNotEqualTo(old.getId());
        assertThat(request.getSession().getAttribute("untrusted")).isNull();
        assertThat(response.getCookie("XSRF-TOKEN").getValue()).isNotBlank();
        assertThat(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .isNotNull();
        registry.onApplicationEvent(new HttpSessionDestroyedEvent(request.getSession()));
        assertThat(registry.getAllPrincipals()).isEmpty();
    }

    @Test
    void simultaneousLoginsLeaveOnlyOneLiveSessionPerAccount() throws Exception {
        var start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 12; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        login(1, new MockHttpServletRequest(), new MockHttpServletResponse());
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                    return null;
                }));
            }
            start.countDown();
            for (var future : futures) {
                future.get();
            }
        }
        login(2, new MockHttpServletRequest(), new MockHttpServletResponse());
        assertThat(registry.getAllSessions(principal(1), false)).hasSize(1);
        assertThat(registry.getAllSessions(principal(1), true)).hasSize(12);
        assertThat(registry.getAllSessions(principal(2), false)).hasSize(1);
    }

    private void login(long id, MockHttpServletRequest request, MockHttpServletResponse response) {
        var user = principal(id);
        manager.login(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()), request,
                response);
    }

    private CustomUserDetails principal(long id) {
        User user = new User();
        user.setId(id);
        user.setRole(UserRole.USER);
        return new CustomUserDetails(user);
    }

    private AuthSessionManager manager() {
        var concurrent = new ConcurrentSessionControlAuthenticationStrategy(registry);
        concurrent.setMaximumSessions(1);
        var fixation = new SessionFixationProtectionStrategy();
        fixation.setMigrateSessionAttributes(false);
        return new AuthSessionManager(
                new CompositeSessionAuthenticationStrategy(
                        List.of(concurrent, fixation, new RegisterSessionAuthenticationStrategy(registry))),
                new HttpSessionSecurityContextRepository(), CookieCsrfTokenRepository.withHttpOnlyFalse());
    }
}
