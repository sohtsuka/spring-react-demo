package com.example.app.service.impl;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.app.exception.AppException;
import com.example.app.exception.ErrorCode;
import com.example.app.model.dto.OnlineBatchJobResponse;
import com.example.app.model.dto.StartOnlineBatchRequest;
import com.example.app.model.entity.OnlineBatchJob;
import com.example.app.model.enums.BatchJobStatus;
import com.example.app.repository.OnlineBatchJobRepository;
import com.example.app.service.OnlineBatchService;

@Service
public class OnlineBatchServiceImpl implements OnlineBatchService {

    private static final int DEFAULT_DELAY_MS = 400;
    private static final int MAX_RECENT_EVENTS = 8;
    private static final TypeReference<List<String>> RECENT_EVENTS_TYPE = new TypeReference<>() {
    };

    private final OnlineBatchJobRepository onlineBatchJobRepository;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;

    @Autowired
    public OnlineBatchServiceImpl(OnlineBatchJobRepository onlineBatchJobRepository) {
        this(onlineBatchJobRepository, Executors.newVirtualThreadPerTaskExecutor(), new ObjectMapper());
    }

    public OnlineBatchServiceImpl(OnlineBatchJobRepository onlineBatchJobRepository, ExecutorService executor,
            ObjectMapper objectMapper) {
        this.onlineBatchJobRepository = onlineBatchJobRepository;
        this.executor = executor;
        this.objectMapper = objectMapper;
    }

    @Override
    public OnlineBatchJobResponse start(StartOnlineBatchRequest request) {
        validateRequest(request);

        OnlineBatchJob job = new OnlineBatchJob();
        job.setJobName(request.jobName());
        job.setStatus(BatchJobStatus.ACCEPTED);
        job.setTotalItems(request.totalItems());
        job.setProcessedItems(0);
        job.setSuccessCount(0);
        job.setFailureCount(0);
        job.setProgressPercent(0);
        job.setFailureAtItem(request.failureAtItem());
        job.setProcessingDelayMs(request.processingDelayMs() == null ? DEFAULT_DELAY_MS : request.processingDelayMs());
        job.setCurrentItem(null);
        job.setRecentEvents(toJson(List.of(eventMessage("ジョブを受け付けました"))));
        onlineBatchJobRepository.insert(job);

        executor.submit(() -> process(job.getId()));
        return findById(job.getId());
    }

    @Override
    public List<OnlineBatchJobResponse> findAll() {
        return onlineBatchJobRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public OnlineBatchJobResponse findById(Long id) {
        return toResponse(getJob(id));
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private void validateRequest(StartOnlineBatchRequest request) {
        if (request.failureAtItem() != null && request.failureAtItem() > request.totalItems()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "失敗させる件番は処理件数以下で指定してください");
        }
    }

    private void process(Long jobId) {
        OnlineBatchJob job = getJob(jobId);
        markRunning(job);
        try {
            for (int itemNo = 1; itemNo <= job.getTotalItems(); itemNo++) {
                job = getJob(jobId);
                startItem(job, itemNo);
                sleep(job.getProcessingDelayMs());

                job = getJob(jobId);
                if (shouldFailAt(job, itemNo)) {
                    markFailed(job, itemNo);
                    return;
                }
                markItemCompleted(job, itemNo);
            }
            markCompleted(getJob(jobId));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            markInterrupted(getJob(jobId));
        }
    }

    private void markRunning(OnlineBatchJob job) {
        job.setStatus(BatchJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        prependEvent(job, "オンラインバッチを開始しました");
        onlineBatchJobRepository.update(job);
    }

    private void startItem(OnlineBatchJob job, int itemNo) {
        job.setCurrentItem(itemLabel(itemNo, job.getTotalItems()));
        prependEvent(job, job.getCurrentItem() + " の処理を開始しました");
        onlineBatchJobRepository.update(job);
    }

    private void markItemCompleted(OnlineBatchJob job, int itemNo) {
        job.setProcessedItems(itemNo);
        job.setSuccessCount(job.getSuccessCount() + 1);
        job.setProgressPercent(progressPercent(itemNo, job.getTotalItems()));
        job.setCurrentItem(itemLabel(itemNo, job.getTotalItems()));
        prependEvent(job, job.getCurrentItem() + " の処理が完了しました");
        onlineBatchJobRepository.update(job);
    }

    private void markFailed(OnlineBatchJob job, int itemNo) {
        job.setProcessedItems(itemNo);
        job.setFailureCount(1);
        job.setProgressPercent(progressPercent(itemNo, job.getTotalItems()));
        job.setStatus(BatchJobStatus.FAILED);
        job.setCurrentItem(itemLabel(itemNo, job.getTotalItems()));
        job.setCompletedAt(LocalDateTime.now());
        prependEvent(job, job.getCurrentItem() + " でエラーが発生し、ジョブを停止しました");
        onlineBatchJobRepository.update(job);
    }

    private void markCompleted(OnlineBatchJob job) {
        job.setStatus(BatchJobStatus.COMPLETED);
        job.setCurrentItem(null);
        job.setCompletedAt(LocalDateTime.now());
        prependEvent(job, "すべてのデータを処理しました");
        onlineBatchJobRepository.update(job);
    }

    private void markInterrupted(OnlineBatchJob job) {
        job.setStatus(BatchJobStatus.FAILED);
        job.setCompletedAt(LocalDateTime.now());
        prependEvent(job, "ジョブが中断されました");
        onlineBatchJobRepository.update(job);
    }

    private OnlineBatchJob getJob(Long id) {
        return onlineBatchJobRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.BATCH_JOB_NOT_FOUND));
    }

    private void prependEvent(OnlineBatchJob job, String message) {
        List<String> events = new ArrayList<>(parseEvents(job.getRecentEvents()));
        events.add(0, eventMessage(message));
        if (events.size() > MAX_RECENT_EVENTS) {
            events = new ArrayList<>(events.subList(0, MAX_RECENT_EVENTS));
        }
        job.setRecentEvents(toJson(events));
    }

    private List<String> parseEvents(String json) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(json, RECENT_EVENTS_TYPE);
        } catch (JsonProcessingException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private String toJson(List<String> events) {
        try {
            return objectMapper.writeValueAsString(events);
        } catch (JsonProcessingException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private OnlineBatchJobResponse toResponse(OnlineBatchJob job) {
        return new OnlineBatchJobResponse(job.getId(), job.getJobName(), job.getStatus(), job.getTotalItems(),
                job.getProcessedItems(), job.getSuccessCount(), job.getFailureCount(), job.getProgressPercent(),
                job.getFailureAtItem(), job.getProcessingDelayMs(), job.getCurrentItem(), job.getCreatedAt(),
                job.getStartedAt(), job.getCompletedAt(), List.copyOf(parseEvents(job.getRecentEvents())));
    }

    private int progressPercent(int processedItems, int totalItems) {
        return totalItems == 0 ? 0 : processedItems * 100 / totalItems;
    }

    private boolean shouldFailAt(OnlineBatchJob job, int itemNo) {
        return job.getFailureAtItem() != null && job.getFailureAtItem() == itemNo;
    }

    private String itemLabel(int itemNo, int totalItems) {
        return "データ" + itemNo + "/" + totalItems;
    }

    private String eventMessage(String message) {
        return LocalDateTime.now() + " " + message;
    }

    private void sleep(int millis) throws InterruptedException {
        if (millis > 0) {
            Thread.sleep(millis);
        }
    }
}
