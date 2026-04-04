package com.example.app.config;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

class SecurityConfigTest {

    OncePerRequestFilter filter;
    Method doFilterInternal;

    @BeforeEach
    void setUp() throws Exception {
        Class<?> filterClass = Arrays.stream(SecurityConfig.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("CsrfCookieFilter")).findFirst().orElseThrow();
        Constructor<?> ctor = filterClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        filter = (OncePerRequestFilter) ctor.newInstance();

        doFilterInternal = filterClass.getDeclaredMethod("doFilterInternal", HttpServletRequest.class,
                HttpServletResponse.class, FilterChain.class);
        doFilterInternal.setAccessible(true);
    }

    @Test
    void doFilterInternal_withNullCsrfToken_continuesChainWithoutCallingGetToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        // CsrfToken 属性を設定しない → csrfToken == null ブランチ
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        doFilterInternal.invoke(filter, request, response, chain);

        then(chain).should().doFilter(request, response);
    }

    @Test
    void doFilterInternal_withNonNullCsrfToken_callsGetTokenAndContinuesChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        CsrfToken csrfToken = mock(CsrfToken.class);
        request.setAttribute(CsrfToken.class.getName(), csrfToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        doFilterInternal.invoke(filter, request, response, chain);

        then(csrfToken).should().getToken();
        then(chain).should().doFilter(request, response);
    }
}
