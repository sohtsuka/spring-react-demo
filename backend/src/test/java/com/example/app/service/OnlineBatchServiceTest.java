package com.example.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.transaction.support.TransactionOperations;

import com.example.app.exception.AppException;
import com.example.app.exception.ErrorCode;
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
            executor, new ObjectMapper(), new BatchAdmission(100, 100, 1000, 1000),
            TransactionOperations.withoutTransaction(), 7, 1000);

    @AfterEach
    void tearDown() {
        onlineBatchService.shutdown();
    }

    @Test
    void start_completesJobAsynchronously() throws Exception {
        OnlineBatchJobResponse accepted = onlineBatchService.start(1L, new StartOnlineBatchRequest("売上集計", 3, null, 1));

        OnlineBatchJobResponse completed = waitUntilFinished(accepted.id());

        assertEquals(BatchJobStatus.COMPLETED, completed.status());
        assertEquals(3, completed.successCount());
        assertEquals(100, completed.progressPercent());
    }

    @Test
    void start_withFailureAtItem_marksJobFailed() throws Exception {
        OnlineBatchJobResponse accepted = onlineBatchService.start(1L, new StartOnlineBatchRequest("失敗デモ", 4, 2, 1));

        OnlineBatchJobResponse failed = waitUntilFinished(accepted.id());

        assertEquals(BatchJobStatus.FAILED, failed.status());
        assertEquals(1, failed.successCount());
        assertEquals(1, failed.failureCount());
        assertEquals(50, failed.progressPercent());
    }

    @Test
    void recoverInterruptedMarksOnlyUnfinishedJobsFailed() {
        Long accepted = onlineBatchJobRepository.seedJob(job -> {
        });
        Long running = onlineBatchJobRepository.seedJob(job -> job.setStatus(BatchJobStatus.RUNNING));
        Long completed = onlineBatchJobRepository.seedJob(job -> job.setStatus(BatchJobStatus.COMPLETED));

        onlineBatchService.recoverInterrupted();

        assertThat(onlineBatchService.findById(accepted).status()).isEqualTo(BatchJobStatus.FAILED);
        assertThat(onlineBatchService.findById(running).status()).isEqualTo(BatchJobStatus.FAILED);
        assertThat(onlineBatchService.findById(completed).status()).isEqualTo(BatchJobStatus.COMPLETED);
        assertThat(onlineBatchService.findById(running).completedAt()).isNotNull();
    }

    @Test
    void processingExceptionMarksJobFailed() throws Exception {
        onlineBatchJobRepository.failNextUpdate.set(true);
        Long id = onlineBatchService.start(1L, new StartOnlineBatchRequest("exception", 1, null, 0)).id();

        OnlineBatchJobResponse failed = waitUntilFinished(id);

        assertThat(failed.status()).isEqualTo(BatchJobStatus.FAILED);
        assertThat(failed.completedAt()).isNotNull();
    }

    @Test
    void failureRecordingExceptionLeavesJobForStartupRecovery() throws Exception {
        onlineBatchJobRepository.failNextUpdate.set(true);
        onlineBatchJobRepository.failNextRecovery.set(true);
        Long id = onlineBatchService.start(1L, new StartOnlineBatchRequest("recovery", 1, null, 0)).id();
        for (int attempt = 0; attempt < 100 && onlineBatchJobRepository.failNextRecovery.get(); attempt++) {
            Thread.sleep(10);
        }

        assertThat(onlineBatchJobRepository.failNextRecovery).isFalse();
        assertThat(onlineBatchService.findById(id).status()).isEqualTo(BatchJobStatus.ACCEPTED);
        onlineBatchService.recoverInterrupted();
        assertThat(onlineBatchService.findById(id).status()).isEqualTo(BatchJobStatus.FAILED);
    }

    @Test
    void start_withFailureAtItemGreaterThanTotal_throwsValidationError() {
        assertThrows(AppException.class,
                () -> onlineBatchService.start(1L, new StartOnlineBatchRequest("不正", 3, 4, 0)));
    }

    @Test
    void start_withNullDelay_usesDefaultDelay() {
        OnlineBatchJobResponse accepted = onlineBatchService.start(1L,
                new StartOnlineBatchRequest("標準遅延", 1, null, null));

        assertEquals(400, accepted.processingDelayMs());
    }

    @Test
    void start_withZeroDelay_completesWithoutSleeping() throws Exception {
        OnlineBatchJobResponse accepted = onlineBatchService.start(1L, new StartOnlineBatchRequest("即時実行", 2, null, 0));

        OnlineBatchJobResponse completed = waitUntilFinished(accepted.id());

        assertEquals(BatchJobStatus.COMPLETED, completed.status());
        assertEquals(0, completed.processingDelayMs());
    }

    @Test
    void findById_withNullRecentEvents_returnsEmptyEvents() {
        Long jobId = onlineBatchJobRepository.seedJob(job -> job.setRecentEvents(null));

        OnlineBatchJobResponse response = onlineBatchService.findById(jobId);

        assertEquals(List.of(), response.recentEvents());
    }

    @Test
    void findById_withBlankRecentEvents_returnsEmptyEvents() {
        Long jobId = onlineBatchJobRepository.seedJob(job -> job.setRecentEvents("   "));

        OnlineBatchJobResponse response = onlineBatchService.findById(jobId);

        assertEquals(List.of(), response.recentEvents());
    }

    @Test
    void progressPercent_withZeroTotal_returnsZero() throws Exception {
        Method method = OnlineBatchServiceImpl.class.getDeclaredMethod("progressPercent", int.class, int.class);
        method.setAccessible(true);

        int progress = (int) method.invoke(onlineBatchService, 3, 0);

        assertEquals(0, progress);
    }

    @Test
    void findAll_returnsJobsSortedByCreatedAtDesc() {
        Long olderId = onlineBatchJobRepository.seedJob(job -> {
            job.setJobName("older");
            job.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        });
        Long newerId = onlineBatchJobRepository.seedJob(job -> job.setJobName("newer"));

        List<OnlineBatchJobResponse> jobs = onlineBatchService.findAll(1, 20).data();

        assertThat(jobs).extracting(OnlineBatchJobResponse::id).containsSubsequence(newerId, olderId);
    }

    @Test
    void findById_whenJobDoesNotExist_throwsNotFound() {
        assertThatThrownBy(() -> onlineBatchService.findById(999L)).isInstanceOf(AppException.class).satisfies(
                ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.BATCH_JOB_NOT_FOUND));
    }

    @Test
    void findById_withInvalidRecentEvents_throwsUncheckedIOException() {
        Long jobId = onlineBatchJobRepository.seedJob(job -> job.setRecentEvents("not-json"));

        assertThatThrownBy(() -> onlineBatchService.findById(jobId)).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void start_whenSerializingEventsFails_throwsUncheckedIOException() throws Exception {
        OnlineBatchJobRepository repository = mock(OnlineBatchJobRepository.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        given(objectMapper.writeValueAsString(any())).willThrow(new StubJsonProcessingException("write failed"));
        OnlineBatchServiceImpl service = new OnlineBatchServiceImpl(repository,
                Executors.newVirtualThreadPerTaskExecutor(), objectMapper, new BatchAdmission(100, 100, 1000, 1000),
                TransactionOperations.withoutTransaction(), 7, 1000);

        try {
            assertThatThrownBy(() -> service.start(1L, new StartOnlineBatchRequest("broken", 1, null, 0)))
                    .isInstanceOf(UncheckedIOException.class);
        } finally {
            service.shutdown();
        }
    }

    @Test
    void paginationRejectsInvalidRangesAndHandlesLargeOffsets() {
        for (int[] range : new int[][]{{0, 20}, {1, 0}, {1, 101}}) {
            assertThatThrownBy(() -> onlineBatchService.findAll(range[0], range[1])).isInstanceOf(AppException.class);
        }
        onlineBatchJobRepository.seedJob(job -> {
        });
        assertThat(onlineBatchService.findAll(1, 1).data()).hasSize(1);
        assertThat(onlineBatchService.findAll(Integer.MAX_VALUE, 100).data()).isEmpty();
    }

    @Test
    void historyFullAndInsertFailureReleaseReservedCapacity() {
        BatchAdmission admission = new BatchAdmission(1, 1, 10, 10);
        OnlineBatchJobRepository repository = mock(OnlineBatchJobRepository.class);
        given(repository.count()).willReturn(1L, 0L);
        org.mockito.Mockito.doThrow(new IllegalStateException("database unavailable")).when(repository).insert(any());
        OnlineBatchServiceImpl service = new OnlineBatchServiceImpl(repository, executor, new ObjectMapper(), admission,
                TransactionOperations.withoutTransaction(), 7, 1);
        assertThatThrownBy(() -> service.start(1, new StartOnlineBatchRequest("full", 1, null, 0)))
                .isInstanceOf(AppException.class).satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.BATCH_CAPACITY_EXCEEDED));
        assertThatThrownBy(() -> service.start(1, new StartOnlineBatchRequest("insert", 1, null, 0)))
                .isInstanceOf(IllegalStateException.class);
        admission.acquire(1).close();
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(1)).insert(any());
    }

    @Test
    void rejectedDispatchMarksFailedAndReleasesCapacity() {
        ExecutorService rejected = mock(ExecutorService.class);
        org.mockito.Mockito.doThrow(new java.util.concurrent.RejectedExecutionException()).when(rejected)
                .execute(any());
        BatchAdmission admission = new BatchAdmission(1, 1, 10, 10);
        OnlineBatchServiceImpl service = new OnlineBatchServiceImpl(onlineBatchJobRepository, rejected,
                new ObjectMapper(), admission, TransactionOperations.withoutTransaction(), 7, 1000);
        assertThatThrownBy(() -> service.start(1, new StartOnlineBatchRequest("rejected", 1, null, 0)))
                .isInstanceOf(AppException.class);
        assertThat(service.findAll(1, 20).data()).extracting(OnlineBatchJobResponse::status)
                .containsExactly(BatchJobStatus.FAILED);
        admission.acquire(1).close();
    }

    @Test
    void invalidHistoryConfigurationIsRejected() {
        for (int[] limits : new int[][]{{0, 1}, {1, 0}}) {
            assertThatThrownBy(() -> new OnlineBatchServiceImpl(onlineBatchJobRepository, executor, new ObjectMapper(),
                    new BatchAdmission(1, 1, 1, 1), TransactionOperations.withoutTransaction(), limits[0], limits[1]))
                    .isInstanceOf(IllegalArgumentException.class);
        }
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
        private final AtomicBoolean failNextUpdate = new AtomicBoolean();
        private final AtomicBoolean failNextRecovery = new AtomicBoolean();

        @Override
        public void failIncomplete(Long id, String events) {
            if (failNextRecovery.compareAndSet(true, false)) {
                throw new IllegalStateException("simulated recovery failure");
            }
            store.values().stream().filter(job -> id == null || id.equals(job.getId())).filter(
                    job -> job.getStatus() == BatchJobStatus.ACCEPTED || job.getStatus() == BatchJobStatus.RUNNING)
                    .forEach(job -> {
                        job.setStatus(BatchJobStatus.FAILED);
                        job.setCompletedAt(LocalDateTime.now());
                        job.setRecentEvents(events);
                        job.setCurrentItem(null);
                    });
        }

        @Override
        public Optional<OnlineBatchJob> findById(Long id) {
            OnlineBatchJob job = store.get(id);
            return job == null ? Optional.empty() : Optional.of(copy(job));
        }

        @Override
        public List<OnlineBatchJob> findAll(long offset, int limit) {
            return store.values().stream().map(this::copy)
                    .sorted(Comparator.comparing(OnlineBatchJob::getCreatedAt).reversed()).skip(offset).limit(limit)
                    .toList();
        }

        @Override
        public long count() {
            return store.size();
        }

        @Override
        public void pruneHistory(LocalDateTime cutoff, int maxRows) {
            store.values().stream()
                    .filter(job -> job.getStatus() == BatchJobStatus.COMPLETED
                            || job.getStatus() == BatchJobStatus.FAILED)
                    .filter(job -> job.getCompletedAt() != null && job.getCompletedAt().isBefore(cutoff))
                    .map(OnlineBatchJob::getId).toList().forEach(store::remove);
            store.values().stream()
                    .filter(job -> job.getStatus() == BatchJobStatus.COMPLETED
                            || job.getStatus() == BatchJobStatus.FAILED)
                    .sorted(Comparator.comparing(OnlineBatchJob::getCreatedAt))
                    .limit(Math.max(0, store.size() - maxRows)).map(OnlineBatchJob::getId).toList()
                    .forEach(store::remove);
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
            if (failNextUpdate.compareAndSet(true, false)) {
                throw new IllegalStateException("simulated update failure");
            }
            OnlineBatchJob copy = copy(job);
            copy.setUpdatedAt(LocalDateTime.now());
            store.put(copy.getId(), copy);
        }

        Long seedJob(java.util.function.Consumer<OnlineBatchJob> customizer) {
            OnlineBatchJob job = new OnlineBatchJob();
            job.setJobName("seed");
            job.setStatus(BatchJobStatus.ACCEPTED);
            job.setTotalItems(1);
            job.setProcessedItems(0);
            job.setSuccessCount(0);
            job.setFailureCount(0);
            job.setProgressPercent(0);
            job.setProcessingDelayMs(0);
            LocalDateTime now = LocalDateTime.now();
            job.setCreatedAt(now);
            job.setUpdatedAt(now);
            customizer.accept(job);
            insert(job);
            return job.getId();
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

    private static final class StubJsonProcessingException extends JsonProcessingException {
        StubJsonProcessingException(String message) {
            super(message);
        }
    }
}
