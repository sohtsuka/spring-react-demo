package com.example.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.example.app.exception.AppException;
import com.example.app.exception.ErrorCode;
import com.example.app.exception.GlobalExceptionHandler;
import com.example.app.model.dto.OnlineBatchJobResponse;
import com.example.app.model.dto.StartOnlineBatchRequest;
import com.example.app.model.enums.BatchJobStatus;
import com.example.app.service.OnlineBatchService;

@ExtendWith(MockitoExtension.class)
class OnlineBatchControllerTest {

    @Mock
    OnlineBatchService onlineBatchService;

    @InjectMocks
    OnlineBatchController onlineBatchController;

    MockMvc mockMvc;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(onlineBatchController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void list_returns200() throws Exception {
        given(onlineBatchService.findAll()).willReturn(List.of(buildResponse(BatchJobStatus.RUNNING)));

        mockMvc.perform(get("/api/v1/online-batch-jobs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].jobName").value("CSV取込デモ"))
                .andExpect(jsonPath("$.data[0].status").value("RUNNING"));
    }

    @Test
    void get_existingJob_returns200() throws Exception {
        given(onlineBatchService.findById(1L)).willReturn(buildResponse(BatchJobStatus.COMPLETED));

        mockMvc.perform(get("/api/v1/online-batch-jobs/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressPercent").value(100));
    }

    @Test
    void get_unknownJob_returns404() throws Exception {
        given(onlineBatchService.findById(99L)).willThrow(new AppException(ErrorCode.BATCH_JOB_NOT_FOUND));

        mockMvc.perform(get("/api/v1/online-batch-jobs/99")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BATCH_JOB_NOT_FOUND"));
    }

    @Test
    void start_withValidRequest_returns201() throws Exception {
        StartOnlineBatchRequest request = new StartOnlineBatchRequest("CSV取込デモ", 6, null, 0);
        given(onlineBatchService.start(any())).willReturn(buildResponse(BatchJobStatus.ACCEPTED));

        mockMvc.perform(post("/api/v1/online-batch-jobs").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
    }

    @Test
    void start_withInvalidRequest_returns400() throws Exception {
        StartOnlineBatchRequest request = new StartOnlineBatchRequest("", 0, null, -1);

        mockMvc.perform(post("/api/v1/online-batch-jobs").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private OnlineBatchJobResponse buildResponse(BatchJobStatus status) {
        int processedItems = status == BatchJobStatus.COMPLETED ? 6 : 3;
        int successCount = status == BatchJobStatus.COMPLETED ? 6 : 3;
        int progressPercent = status == BatchJobStatus.COMPLETED ? 100 : 50;
        return new OnlineBatchJobResponse(1L, "CSV取込デモ", status, 6, processedItems, successCount, 0, progressPercent,
                null, 0, status == BatchJobStatus.RUNNING ? "データ4/6" : null, LocalDateTime.now(), LocalDateTime.now(),
                status == BatchJobStatus.COMPLETED ? LocalDateTime.now() : null,
                List.of("2026-04-25T10:00:00 ジョブを受け付けました"));
    }
}
