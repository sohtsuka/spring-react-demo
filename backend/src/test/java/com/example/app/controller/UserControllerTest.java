package com.example.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.app.exception.GlobalExceptionHandler;
import com.example.app.model.dto.CreateUserRequest;
import com.example.app.model.dto.PagedResponse;
import com.example.app.model.dto.UpdateUserRequest;
import com.example.app.model.dto.UserResponse;
import com.example.app.model.enums.UserRole;
import com.example.app.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    UserService userService;

    @InjectMocks
    UserController userController;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(userController).setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void list_returns200() throws Exception {
        UserResponse user = buildUserResponse();
        given(userService.findAll(1, 20)).willReturn(PagedResponse.of(List.of(user), 1, 20, 1));

        mockMvc.perform(get("/api/v1/users")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("testuser"))
                .andExpect(jsonPath("$.pagination.totalElements").value(1));
    }

    @Test
    void get_existingUser_returns200() throws Exception {
        given(userService.findById(1L)).willReturn(buildUserResponse());

        mockMvc.perform(get("/api/v1/users/1")).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void create_withValidRequest_returns201() throws Exception {
        CreateUserRequest request = new CreateUserRequest("newuser", "new@example.com", "Password1!", UserRole.USER);
        given(userService.create(any())).willReturn(buildUserResponse());

        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void create_withInvalidPassword_returns400() throws Exception {
        CreateUserRequest request = new CreateUserRequest("newuser", "new@example.com", "weak", UserRole.USER);

        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void update_existingUser_returns200() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest(null, null, UserRole.MANAGER, null);
        given(userService.update(eq(1L), any())).willReturn(buildUserResponse());

        mockMvc.perform(put("/api/v1/users/1").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isOk());
    }

    @Test
    void delete_existingUser_returns204() throws Exception {
        willDoNothing().given(userService).delete(1L);

        mockMvc.perform(delete("/api/v1/users/1")).andExpect(status().isNoContent());
    }

    private UserResponse buildUserResponse() {
        return new UserResponse(1L, "testuser", "test@example.com", UserRole.USER, true, LocalDateTime.now(),
                LocalDateTime.now());
    }
}
