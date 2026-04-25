package com.example.app.model.entity;

import java.time.LocalDateTime;

import com.example.app.model.enums.BatchJobStatus;

public class OnlineBatchJob {

    private Long id;
    private String jobName;
    private BatchJobStatus status;
    private int totalItems;
    private int processedItems;
    private int successCount;
    private int failureCount;
    private int progressPercent;
    private Integer failureAtItem;
    private int processingDelayMs;
    private String currentItem;
    private String recentEvents;
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public BatchJobStatus getStatus() {
        return status;
    }

    public void setStatus(BatchJobStatus status) {
        this.status = status;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getProcessedItems() {
        return processedItems;
    }

    public void setProcessedItems(int processedItems) {
        this.processedItems = processedItems;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getFailureAtItem() {
        return failureAtItem;
    }

    public void setFailureAtItem(Integer failureAtItem) {
        this.failureAtItem = failureAtItem;
    }

    public int getProcessingDelayMs() {
        return processingDelayMs;
    }

    public void setProcessingDelayMs(int processingDelayMs) {
        this.processingDelayMs = processingDelayMs;
    }

    public String getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(String currentItem) {
        this.currentItem = currentItem;
    }

    public String getRecentEvents() {
        return recentEvents;
    }

    public void setRecentEvents(String recentEvents) {
        this.recentEvents = recentEvents;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
