package com.example.app.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.app.model.dto.ErrorResponse;
import com.example.app.model.entity.User;
import com.example.app.repository.UserRepository;

/** 認可前に、セッションに保存された権限を現在のDBと照合する。 */
public class SessionValidationFilter extends OncePerRequestFilter {
    private final UserRepository users;
    private final ObjectMapper mapper = new ObjectMapper();

    public SessionValidationFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails principal) {
            try {
                User previous = principal.getUser();
                boolean valid = users.findById(previous.getId()).filter(current -> valid(previous, current))
                        .isPresent();
                if (!valid) {
                    var session = request.getSession(false);
                    if (session != null) {
                        session.invalidate();
                    }
                    SecurityContextHolder.clearContext();
                }
            } catch (DataAccessException ex) {
                logger.error("Session validation failed", ex);
                response.setStatus(500);
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(
                        mapper.writeValueAsString(ErrorResponse.of("INTERNAL_SERVER_ERROR", "サーバー内部エラーが発生しました")));
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private boolean valid(User previous, User current) {
        return current.isEnabled()
                && (current.getLockedUntil() == null || current.getLockedUntil().isBefore(LocalDateTime.now()))
                && current.getRole() == previous.getRole()
                && Objects.equals(current.getUsername(), previous.getUsername())
                && Objects.equals(current.getPassword(), previous.getPassword());
    }
}
