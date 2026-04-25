package com.example.app.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import com.example.app.exception.AppException;
import com.example.app.exception.ErrorCode;
import com.example.app.model.dto.OnlineBatchJobResponse;
import com.example.app.model.dto.StartOnlineBatchRequest;
import com.example.app.model.enums.BatchJobStatus;
import com.example.app.service.OnlineBatchService;

@Service
public class OnlineBatchServiceImpl implements OnlineBatchService {

    private static final int DEFAULT_DELAY_MS = 400;
    private static final int MAX_RECENT_EVENTS = 8;

    private final ConcurrentMap<Long, BatchJob> jobs = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public OnlineBatchJobResponse start(StartOnlineBatchRequest request) {
        validateRequest(request);

        long id = sequence.incrementAndGet();
        int processingDelayMs = request.processingDelayMs() == null ? DEFAULT_DELAY_MS : request.processingDelayMs();

        BatchJob job = new BatchJob(id, request.jobName(), request.totalItems(), request.failureAtItem(),
                processingDelayMs);
        jobs.put(id, job);
        executor.submit(() -> process(job));
        return job.toResponse();
    }

    @Override
    public List<OnlineBatchJobResponse> findAll() {
        return jobs.values().stream().map(BatchJob::toResponse)
                .sorted(Comparator.comparing(OnlineBatchJobResponse::createdAt).reversed()).toList();
    }

    @Override
    public OnlineBatchJobResponse findById(Long id) {
        BatchJob job = jobs.get(id);
        if (job == null) {
            throw new AppException(ErrorCode.BATCH_JOB_NOT_FOUND);
        }
        return job.toResponse();
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

    private void process(BatchJob job) {
        job.markRunning();
        try {
            for (int itemNo = 1; itemNo <= job.totalItems(); itemNo++) {
                job.startItem(itemNo);
                sleep(job.processingDelayMs());
                if (job.shouldFailAt(itemNo)) {
                    job.markFailed(itemNo);
                    return;
                }
                job.markItemCompleted(itemNo);
            }
            job.markCompleted();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            job.markInterrupted();
        }
    }

    private void sleep(int millis) throws InterruptedException {
        if (millis > 0) {
            Thread.sleep(millis);
        }
    }

    private static final class BatchJob {
        private final Long id;
        private final String jobName;
        private final int totalItems;
        private final Integer failureAtItem;
        private final int processingDelayMs;
        private final LocalDateTime createdAt;
        private final List<String> recentEvents = new ArrayList<>();

        private BatchJobStatus status;
        private int processedItems;
        private int successCount;
        private int failureCount;
        private String currentItem;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;

        private BatchJob(Long id, String jobName, int totalItems, Integer failureAtItem, int processingDelayMs) {
            this.id = id;
            this.jobName = jobName;
            this.totalItems = totalItems;
            this.failureAtItem = failureAtItem;
            this.processingDelayMs = processingDelayMs;
            this.createdAt = LocalDateTime.now();
            this.status = BatchJobStatus.ACCEPTED;
            addEvent("ジョブを受け付けました");
        }

        private synchronized void markRunning() {
            status = BatchJobStatus.RUNNING;
            startedAt = LocalDateTime.now();
            addEvent("オンラインバッチを開始しました");
        }

        private synchronized void startItem(int itemNo) {
            currentItem = itemLabel(itemNo);
            addEvent(currentItem + " の処理を開始しました");
        }

        private synchronized void markItemCompleted(int itemNo) {
            processedItems = itemNo;
            successCount++;
            currentItem = itemLabel(itemNo);
            addEvent(currentItem + " の処理が完了しました");
        }

        private synchronized void markFailed(int itemNo) {
            processedItems = itemNo;
            failureCount = 1;
            status = BatchJobStatus.FAILED;
            currentItem = itemLabel(itemNo);
            completedAt = LocalDateTime.now();
            addEvent(currentItem + " でエラーが発生し、ジョブを停止しました");
        }

        private synchronized void markCompleted() {
            status = BatchJobStatus.COMPLETED;
            currentItem = null;
            completedAt = LocalDateTime.now();
            addEvent("すべてのデータを処理しました");
        }

        private synchronized void markInterrupted() {
            status = BatchJobStatus.FAILED;
            completedAt = LocalDateTime.now();
            addEvent("ジョブが中断されました");
        }

        private synchronized OnlineBatchJobResponse toResponse() {
            return new OnlineBatchJobResponse(id, jobName, status, totalItems, processedItems, successCount,
                    failureCount, totalItems == 0 ? 0 : processedItems * 100 / totalItems, failureAtItem,
                    processingDelayMs, currentItem, createdAt, startedAt, completedAt, List.copyOf(recentEvents));
        }

        private synchronized boolean shouldFailAt(int itemNo) {
            return failureAtItem != null && failureAtItem == itemNo;
        }

        private int totalItems() {
            return totalItems;
        }

        private int processingDelayMs() {
            return processingDelayMs;
        }

        private String itemLabel(int itemNo) {
            return "データ" + itemNo + "/" + totalItems;
        }

        private void addEvent(String message) {
            recentEvents.add(0, String.format("%s %s", LocalDateTime.now(), message));
            if (recentEvents.size() > MAX_RECENT_EVENTS) {
                recentEvents.remove(recentEvents.size() - 1);
            }
        }
    }
}
