package team.hotpotato.infrastructure.snowflake;

import team.hotpotato.common.identity.IdGenerator;

public class Snowflake implements IdGenerator {
    private static final long WORKER_BITS = 5L;
    private static final long SEQ_BITS = 12L;

    private static final long WORKER_SHIFT = SEQ_BITS;
    private static final long TIME_SHIFT = SEQ_BITS + WORKER_BITS;

    // 2026-01-01 00:00:00 epoch
    private static final long EPOCH = 1767225600000L;

    private final long workerId;
    private long seq = 0L;
    private long lastTime = -1L;

    public Snowflake(long workerId) {
        this.workerId = workerId;
    }

    public synchronized long generateId() {
        long now = now();

        if (now < lastTime) {
            throw ClockMovedBackwardsException.EXCEPTION;
        }

        if (now == lastTime) {
            seq = (seq + 1) & ((1 << SEQ_BITS) - 1);
            if (seq == 0) {
                now = waitNext(now);
            }
        } else {
            seq = 0L;
        }

        lastTime = now;

        return ((now - EPOCH) << TIME_SHIFT)
                | (workerId << WORKER_SHIFT)
                | seq;
    }

    private long waitNext(long last) {
        long cur = now();
        while (cur <= last) {
            cur = now();
        }
        return cur;
    }

    private long now() {
        return System.currentTimeMillis();
    }
}
