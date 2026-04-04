package com.example.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.app.model.entity.User;
import com.example.app.model.enums.UserRole;
import com.example.app.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    CustomUserDetailsService service;

    @Test
    void loadUserByUsername_existingUser_returnsCustomUserDetails() {
        User user = buildUser("alice");
        given(userRepository.findByUsername("alice")).willReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("alice");

        assertThat(details).isInstanceOf(CustomUserDetails.class);
        assertThat(details.getUsername()).isEqualTo("alice");
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost")).isInstanceOf(UsernameNotFoundException.class);
    }

    private User buildUser(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("hashed");
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        return user;
    }
}
