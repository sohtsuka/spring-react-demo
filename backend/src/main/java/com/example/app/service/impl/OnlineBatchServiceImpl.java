package com.example.app.service.impl;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import com.example.app.exception.AppException;
import com.example.app.exception.ErrorCode;
import com.example.app.model.dto.OnlineBatchJobResponse;
import com.example.app.model.dto.PagedResponse;
import com.example.app.model.dto.StartOnlineBatchRequest;
import com.example.app.model.entity.OnlineBatchJob;
import com.example.app.model.enums.BatchJobStatus;
import com.example.app.repository.OnlineBatchJobRepository;
import com.example.app.service.BatchAdmission;
import com.example.app.service.OnlineBatchService;

@Service
@DependsOn("flyway")
public class OnlineBatchServiceImpl implements OnlineBatchService {

    private static final Logger LOG = LoggerFactory.getLogger(OnlineBatchServiceImpl.class);

    private static final int DEFAULT_DELAY_MS = 400;
    private static final int MAX_RECENT_EVENTS = 8;

    private final OnlineBatchJobRepository onlineBatchJobRepository;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;

    private final BatchAdmission admission;
    private final TransactionOperations transactions;
    private final int historyDays;
    private final int maxHistory;
    private final Object historyLock = new Object();

    @Autowired
    public OnlineBatchServiceImpl(OnlineBatchJobRepository onlineBatchJobRepository, BatchAdmission admission,
            PlatformTransactionManager transactionManager, @Value("${app.batch.history-days:7}") int historyDays,
            @Value("${app.batch.max-history:1000}") int maxHistory) {
        this(onlineBatchJobRepository, Executors.newVirtualThreadPerTaskExecutor(), new ObjectMapper(), admission,
                new TransactionTemplate(transactionManager), historyDays, maxHistory);
    }

    public OnlineBatchServiceImpl(OnlineBatchJobRepository onlineBatchJobRepository, ExecutorService executor,
            ObjectMapper objectMapper, BatchAdmission admission, TransactionOperations transactions, int historyDays,
            int maxHistory) {
        Assert.isTrue(historyDays > 0 && maxHistory > 0, "History limits must be positive");
        this.onlineBatchJobRepository = onlineBatchJobRepository;
        this.executor = executor;
        this.objectMapper = objectMapper;
        this.admission = admission;
        this.transactions = transactions;
        this.historyDays = historyDays;
        this.maxHistory = maxHistory;
    }

    @Override
    public OnlineBatchJobResponse start(long userId, StartOnlineBatchRequest request) {
        validateRequest(request);
        BatchAdmission.Permit permit = admission.acquire(userId);
        OnlineBatchJobResponse accepted;
        try {
            // 単一プロセス内で保持枠確認と INSERT を直列化し、コミット後にのみ実行する。
            synchronized (historyLock) {
                accepted = transactions.execute(status -> {
                    onlineBatchJobRepository.pruneHistory(LocalDateTime.now().minusDays(historyDays), maxHistory - 1);
                    if (onlineBatchJobRepository.count() >= maxHistory) {
                        throw new AppException(ErrorCode.BATCH_CAPACITY_EXCEEDED);
                    }
                    return insert(request);
                });
            }
        } catch (RuntimeException ex) {
            permit.close();
            throw ex;
        }
        try {
            executor.execute(() -> {
                try (permit) {
                    try {
                        process(accepted.id());
                    } catch (RuntimeException ex) {
                        recordFailure(accepted.id(), ex);
                    }
                }
            });
        } catch (RejectedExecutionException ex) {
            permit.close();
            recordFailure(accepted.id(), ex);
            throw new AppException(ErrorCode.BATCH_CAPACITY_EXCEEDED);
        }
        return accepted;
    }

    private OnlineBatchJobResponse insert(StartOnlineBatchRequest request) {
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
        return findById(job.getId());
    }

    private void recordFailure(Long jobId, RuntimeException ex) {
        LOG.error("Batch {} failed", jobId, ex);
        try {
            onlineBatchJobRepository.failIncomplete(jobId, toJson(List.of(eventMessage("ジョブの処理に失敗しました"))));
        } catch (RuntimeException recordingFailure) {
            LOG.error("Cannot record failure for batch {}; it will be recovered at restart", jobId, recordingFailure);
        }
    }

    @Override
    public PagedResponse<OnlineBatchJobResponse> findAll(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
        List<OnlineBatchJobResponse> jobs = onlineBatchJobRepository.findAll(((long) page - 1) * size, size).stream()
                .map(this::toResponse).toList();
        return PagedResponse.of(jobs, page, size, onlineBatchJobRepository.count());
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanHistory() {
        synchronized (historyLock) {
            transactions.executeWithoutResult(status -> onlineBatchJobRepository
                    .pruneHistory(LocalDateTime.now().minusDays(historyDays), maxHistory));
        }
    }

    @Override
    public OnlineBatchJobResponse findById(Long id) {
        return toResponse(getJob(id));
    }

    @PreDestroy
    public void shutdown() {
        admission.stop();
        executor.shutdownNow();
    }

    /** 単一APIプロセス構成。新しいジョブを受け付ける前に前回の未完了ジョブを回収する。 */
    @PostConstruct
    public void recoverInterrupted() {
        onlineBatchJobRepository.failIncomplete(null, toJson(List.of(eventMessage("再起動により未完了ジョブを失敗として回収しました"))));
        cleanHistory();
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
            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
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
