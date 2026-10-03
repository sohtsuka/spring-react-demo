package com.example.app.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.app.model.dto.ApiResponse;
import com.example.app.model.dto.OnlineBatchJobResponse;
import com.example.app.model.dto.PagedResponse;
import com.example.app.model.dto.StartOnlineBatchRequest;
import com.example.app.security.CustomUserDetails;
import com.example.app.service.OnlineBatchService;

@RestController
@RequestMapping("/api/v1/online-batch-jobs")
public class OnlineBatchController {

    private final OnlineBatchService onlineBatchService;

    public OnlineBatchController(OnlineBatchService onlineBatchService) {
        this.onlineBatchService = onlineBatchService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<OnlineBatchJobResponse>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(onlineBatchService.findAll(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OnlineBatchJobResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.of(onlineBatchService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OnlineBatchJobResponse>> start(
            @Valid @RequestBody StartOnlineBatchRequest request, @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(onlineBatchService.start(principal.getAccountId(), request)));
    }
}
