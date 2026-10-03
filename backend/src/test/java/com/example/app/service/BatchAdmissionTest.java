package com.example.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.example.app.exception.AppException;
import com.example.app.exception.ErrorCode;

class BatchAdmissionTest {
    @ParameterizedTest
    @CsvSource({"0,1,1,1", "1,0,1,1", "1,1,0,1", "1,1,1,0"})
    void rejectsInvalidConfiguration(int global, int user, int rate, int tracked) {
        assertThatThrownBy(() -> new BatchAdmission(global, user, rate, tracked))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void perUserAndGlobalLimitsReleaseExactlyOnce() {
        BatchAdmission admission = new BatchAdmission(2, 1, 10, 100);
        var first = admission.acquire(1);
        rejects(admission, 1, ErrorCode.BATCH_USER_LIMIT_EXCEEDED);
        var second = admission.acquire(2);
        rejects(admission, 3, ErrorCode.BATCH_CAPACITY_EXCEEDED);
        first.close();
        first.close();
        var third = admission.acquire(3);
        rejects(admission, 4, ErrorCode.BATCH_CAPACITY_EXCEEDED);
        second.close();
        third.close();
        admission.stop();
        rejects(admission, 1, ErrorCode.BATCH_CAPACITY_EXCEEDED);
    }

    @Test
    void rollingWindowAndBoundedUserStateDoNotEvictActiveUsers() {
        Clock clock = mock(Clock.class);
        Instant now = Instant.parse("2026-10-03T00:00:00Z");
        when(clock.instant()).thenReturn(now);
        BatchAdmission admission = new BatchAdmission(4, 2, 2, 1, clock);
        var active = admission.acquire(1);
        admission.acquire(1).close();
        rejects(admission, 1, ErrorCode.BATCH_USER_LIMIT_EXCEEDED);
        rejects(admission, 2, ErrorCode.BATCH_CAPACITY_EXCEEDED);
        when(clock.instant()).thenReturn(now.plusSeconds(59));
        rejects(admission, 1, ErrorCode.BATCH_USER_LIMIT_EXCEEDED);
        when(clock.instant()).thenReturn(now.plusSeconds(60));
        rejects(admission, 2, ErrorCode.BATCH_CAPACITY_EXCEEDED);
        active.close();
        admission.acquire(2).close();
        when(clock.instant()).thenReturn(now.plusSeconds(120));
        admission.acquire(3).close();
    }

    @Test
    void simultaneousRequestsCannotOverbook() throws Exception {
        BatchAdmission admission = new BatchAdmission(4, 2, 10, 100);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<BatchAdmission.Permit>> results = new ArrayList<>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (long user = 0; user < 32; user++) {
                long id = user;
                results.add(executor.submit(() -> {
                    start.await();
                    try {
                        return admission.acquire(id);
                    } catch (AppException ex) {
                        return null;
                    }
                }));
            }
            start.countDown();
            List<BatchAdmission.Permit> permits = new ArrayList<>();
            for (var result : results) {
                var permit = result.get();
                if (permit != null) {
                    permits.add(permit);
                }
            }
            assertThat(permits).hasSize(4);
            permits.forEach(BatchAdmission.Permit::close);
            admission.acquire(99).close();
        }
    }

    private void rejects(BatchAdmission admission, long user, ErrorCode code) {
        assertThatThrownBy(() -> admission.acquire(user)).isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(code));
    }
}
