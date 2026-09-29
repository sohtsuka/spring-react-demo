package com.example.app.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.example.app.model.entity.User;
import com.example.app.model.enums.UserRole;
import com.example.app.repository.UserRepository;
import com.example.app.security.CustomUserDetails;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class SecurityConfigIntegrationTest {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    WebApplicationContext wac;

    @Autowired
    UserRepository users;

    private static final AtomicInteger IDS = new AtomicInteger();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).apply(springSecurity()).build();
    }

    /**
     * 未認証リクエスト → ExceptionTranslationFilter が authenticationEntryPoint を呼び出す
     * → 401 + {"code":"UNAUTHORIZED"} JSON
     */
    @Test
    void unauthenticated_request_invokes_authenticationEntryPoint() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED")).andExpect(jsonPath("$.message").value("認証が必要です"));
    }

    /**
     * USER ロールで認証済み → フィルターチェーンの hasAnyRole("ADMIN","MANAGER") で拒否
     * → ExceptionTranslationFilter が accessDeniedHandler を呼び出す
     * → 403 + {"code":"FORBIDDEN"} JSON
     */
    @Test
    void authenticated_as_user_role_invokes_accessDeniedHandler() throws Exception {
        mockMvc.perform(get("/api/v1/users").with(user("testuser").roles("USER"))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("この操作を行う権限がありません"));
    }

    /** API は JSON しか返さないため、ブラウザーが何も読み込まない CSP と埋め込み禁止を返す */
    @Test
    void responses_carry_security_headers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(header().string("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void disabledUserLosesExistingSession() throws Exception {
        User current = createAdmin();
        MockHttpSession session = authenticatedSession(current);
        mockMvc.perform(get("/api/v1/users").session(session)).andExpect(status().isOk());

        User changed = users.findById(current.getId()).orElseThrow();
        changed.setEnabled(false);
        users.update(changed);

        mockMvc.perform(get("/api/v1/users").session(session)).andExpect(status().isUnauthorized());
        assertThatThrownBy(() -> session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void demotedUserLosesExistingSession() throws Exception {
        User current = createAdmin();
        MockHttpSession session = authenticatedSession(current);
        User changed = users.findById(current.getId()).orElseThrow();
        changed.setRole(UserRole.USER);
        users.update(changed);

        mockMvc.perform(get("/api/v1/users").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void deletedUserLosesExistingSession() throws Exception {
        User current = createAdmin();
        MockHttpSession session = authenticatedSession(current);
        users.deleteById(current.getId());

        mockMvc.perform(get("/api/v1/users").session(session)).andExpect(status().isUnauthorized());
    }

    private User createAdmin() {
        int id = IDS.incrementAndGet();
        User user = new User();
        user.setUsername("session-admin-" + id);
        user.setEmail("session-admin-" + id + "@example.com");
        user.setPassword("test-hash");
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        users.insert(user);
        return users.findById(user.getId()).orElseThrow();
    }

    private MockHttpSession authenticatedSession(User user) {
        var details = new CustomUserDetails(user);
        var authentication = UsernamePasswordAuthenticationToken.authenticated(details, null, details.getAuthorities());
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
