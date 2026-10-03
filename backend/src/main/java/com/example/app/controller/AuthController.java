package com.example.app.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.exception.AppException;
import com.example.app.exception.ErrorCode;
import com.example.app.model.dto.ApiResponse;
import com.example.app.model.dto.LoginRequest;
import com.example.app.model.dto.UserResponse;
import com.example.app.security.AuthSessionManager;
import com.example.app.security.CustomUserDetails;
import com.example.app.service.UserService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AuthSessionManager sessions;

    public AuthController(AuthenticationManager authenticationManager, UserService userService,
            AuthSessionManager sessions) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.sessions = sessions;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        Authentication auth;
        try {
            auth = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (LockedException ex) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        } catch (DisabledException ex) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        } catch (BadCredentialsException ex) {
            userService.recordLoginFailure(request.username());
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        userService.resetLoginAttempts(request.username());
        sessions.login(auth, httpRequest, httpResponse);

        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.of(UserResponse.from(userDetails.getUser())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return ResponseEntity.ok(ApiResponse.of(UserResponse.from(userDetails.getUser())));
    }
}
