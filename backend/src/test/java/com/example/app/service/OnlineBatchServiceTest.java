package com.example.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.example.app.exception.AppException;
import com.example.app.model.dto.OnlineBatchJobResponse;
import com.example.app.model.dto.StartOnlineBatchRequest;
import com.example.app.model.entity.OnlineBatchJob;
import com.example.app.model.enums.BatchJobStatus;
import com.example.app.repository.OnlineBatchJobRepository;
import com.example.app.service.impl.OnlineBatchServiceImpl;

class OnlineBatchServiceTest {

    private final InMemoryOnlineBatchJobRepository onlineBatchJobRepository = new InMemoryOnlineBatchJobRepository();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final OnlineBatchServiceImpl onlineBatchService = new OnlineBatchServiceImpl(onlineBatchJobRepository,
            executor, new ObjectMapper());

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

    private static final class InMemoryOnlineBatchJobRepository implements OnlineBatchJobRepository {
        private final ConcurrentMap<Long, OnlineBatchJob> store = new ConcurrentHashMap<>();
        private final AtomicLong sequence = new AtomicLong(0);

        @Override
        public Optional<OnlineBatchJob> findById(Long id) {
            OnlineBatchJob job = store.get(id);
            return job == null ? Optional.empty() : Optional.of(copy(job));
        }

        @Override
        public List<OnlineBatchJob> findAll() {
            return store.values().stream().map(this::copy)
                    .sorted(Comparator.comparing(OnlineBatchJob::getCreatedAt).reversed()).toList();
        }

        @Override
        public void insert(OnlineBatchJob job) {
            OnlineBatchJob copy = copy(job);
            copy.setId(sequence.incrementAndGet());
            LocalDateTime now = LocalDateTime.now();
            copy.setCreatedAt(now);
            copy.setUpdatedAt(now);
            job.setId(copy.getId());
            job.setCreatedAt(copy.getCreatedAt());
            job.setUpdatedAt(copy.getUpdatedAt());
            store.put(copy.getId(), copy);
        }

        @Override
        public void update(OnlineBatchJob job) {
            OnlineBatchJob copy = copy(job);
            copy.setUpdatedAt(LocalDateTime.now());
            store.put(copy.getId(), copy);
        }

        private OnlineBatchJob copy(OnlineBatchJob source) {
            OnlineBatchJob copy = new OnlineBatchJob();
            copy.setId(source.getId());
            copy.setJobName(source.getJobName());
            copy.setStatus(source.getStatus());
            copy.setTotalItems(source.getTotalItems());
            copy.setProcessedItems(source.getProcessedItems());
            copy.setSuccessCount(source.getSuccessCount());
            copy.setFailureCount(source.getFailureCount());
            copy.setProgressPercent(source.getProgressPercent());
            copy.setFailureAtItem(source.getFailureAtItem());
            copy.setProcessingDelayMs(source.getProcessingDelayMs());
            copy.setCurrentItem(source.getCurrentItem());
            copy.setRecentEvents(source.getRecentEvents());
            copy.setCreatedAt(source.getCreatedAt());
            copy.setStartedAt(source.getStartedAt());
            copy.setCompletedAt(source.getCompletedAt());
            copy.setUpdatedAt(source.getUpdatedAt());
            return copy;
        }
    }
}
