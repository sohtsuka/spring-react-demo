package com.example.app.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.example.app.exception.AppException;
import com.example.app.exception.ErrorCode;

/** 単一 API プロセスの受付枠。保存・投入中も実行枠に含める。 */
@Component
public class BatchAdmission {
    private final int globalLimit;
    private final int userLimit;
    private final int rateLimit;
    private final int trackedUsers;
    private final Clock clock;
    private final Map<Long, Usage> usage = new HashMap<>();
    private int active;
    private boolean stopped;

    @Autowired
    public BatchAdmission(@Value("${app.batch.max-active:4}") int globalLimit,
            @Value("${app.batch.max-active-per-user:2}") int userLimit,
            @Value("${app.batch.starts-per-minute:10}") int rateLimit,
            @Value("${app.batch.max-tracked-users:10000}") int trackedUsers) {
        this(globalLimit, userLimit, rateLimit, trackedUsers, Clock.systemUTC());
    }

    BatchAdmission(int globalLimit, int userLimit, int rateLimit, int trackedUsers, Clock clock) {
        Assert.isTrue(globalLimit > 0 && userLimit > 0 && rateLimit > 0 && trackedUsers > 0,
                "Batch limits must be positive");
        this.globalLimit = globalLimit;
        this.userLimit = userLimit;
        this.rateLimit = rateLimit;
        this.trackedUsers = trackedUsers;
        this.clock = clock;
    }

    public synchronized Permit acquire(long userId) {
        if (stopped || active >= globalLimit) {
            throw new AppException(ErrorCode.BATCH_CAPACITY_EXCEEDED);
        }
        Instant cutoff = clock.instant().minusSeconds(60);
        usage.values().forEach(value -> value.starts.removeIf(start -> !start.isAfter(cutoff)));
        usage.values().removeIf(value -> value.active == 0 && value.starts.isEmpty());
        if (!usage.containsKey(userId) && usage.size() >= trackedUsers) {
            throw new AppException(ErrorCode.BATCH_CAPACITY_EXCEEDED);
        }
        Usage current = usage.computeIfAbsent(userId, ignored -> new Usage());
        if (current.active >= userLimit || current.starts.size() >= rateLimit) {
            throw new AppException(ErrorCode.BATCH_USER_LIMIT_EXCEEDED);
        }
        current.starts.addLast(clock.instant());
        current.active++;
        active++;
        return new Permit(userId);
    }

    public synchronized void stop() {
        stopped = true;
    }

    private static final class Usage {
        private final ArrayDeque<Instant> starts = new ArrayDeque<>();
        private int active;
    }

    public final class Permit implements AutoCloseable {
        private final long userId;
        private boolean released;

        private Permit(long userId) {
            this.userId = userId;
        }

        @Override
        public void close() {
            synchronized (BatchAdmission.this) {
                if (!released) {
                    usage.get(userId).active--;
                    active--;
                    released = true;
                }
            }
        }
    }
}
