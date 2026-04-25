package com.example.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.example.app.exception.AppException;
import com.example.app.model.dto.OnlineBatchJobResponse;
import com.example.app.model.dto.StartOnlineBatchRequest;
import com.example.app.model.enums.BatchJobStatus;
import com.example.app.service.impl.OnlineBatchServiceImpl;

class OnlineBatchServiceTest {

    private final OnlineBatchServiceImpl onlineBatchService = new OnlineBatchServiceImpl();

    @AfterEach
    void tearDown() {
        onlineBatchService.shutdown();
    }

    @Test
    void start_completesJobAsynchronously() throws Exception {
        OnlineBatchJobResponse accepted = onlineBatchService.start(new StartOnlineBatchRequest("売上集計", 3, null, 1));

        OnlineBatchJobResponse completed = waitUntilFinished(accepted.id());

        assertEquals(BatchJobStatus.COMPLETED, completed.status());
        assertEquals(3, completed.successCount());
        assertEquals(100, completed.progressPercent());
    }

    @Test
    void start_withFailureAtItem_marksJobFailed() throws Exception {
        OnlineBatchJobResponse accepted = onlineBatchService.start(new StartOnlineBatchRequest("失敗デモ", 4, 2, 1));

        OnlineBatchJobResponse failed = waitUntilFinished(accepted.id());

        assertEquals(BatchJobStatus.FAILED, failed.status());
        assertEquals(1, failed.successCount());
        assertEquals(1, failed.failureCount());
        assertEquals(50, failed.progressPercent());
    }

    @Test
    void start_withFailureAtItemGreaterThanTotal_throwsValidationError() {
        assertThrows(AppException.class, () -> onlineBatchService.start(new StartOnlineBatchRequest("不正", 3, 4, 0)));
    }

    private OnlineBatchJobResponse waitUntilFinished(Long jobId) throws Exception {
        for (int i = 0; i < 100; i++) {
            OnlineBatchJobResponse response = onlineBatchService.findById(jobId);
            if (response.status() == BatchJobStatus.COMPLETED || response.status() == BatchJobStatus.FAILED) {
                return response;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("ジョブが完了しませんでした");
    }
}
