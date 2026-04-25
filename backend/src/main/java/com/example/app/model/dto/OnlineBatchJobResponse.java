package com.example.app.model.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.app.model.enums.BatchJobStatus;

public record OnlineBatchJobResponse(Long id, String jobName, BatchJobStatus status, int totalItems, int processedItems,
        int successCount, int failureCount, int progressPercent, Integer failureAtItem, int processingDelayMs,
        String currentItem, LocalDateTime createdAt, LocalDateTime startedAt, LocalDateTime completedAt,
        List<String> recentEvents) {
}
