package com.example.score.service.ratelimit;

import java.time.Duration;
import java.time.Instant;

/**
 * 单条失败计数记录：失败次数、锁定截止时间与最后一次失败时间。
 */
public class AttemptRecord {

    private int count;
    private Instant lockedUntil = Instant.EPOCH;
    private Instant touchedAt = Instant.EPOCH;

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Instant getTouchedAt() {
        return touchedAt;
    }

    public void setTouchedAt(Instant touchedAt) {
        this.touchedAt = touchedAt;
    }

    /**
     * 锁定期与静默窗口都已过期，可以重新计数。
     */
    public boolean isExpired(Instant now, long windowMinutes) {
        return !lockedUntil.isAfter(now)
                && touchedAt.plus(Duration.ofMinutes(windowMinutes)).isBefore(now);
    }

    public void reset(Instant now) {
        this.count = 0;
        this.lockedUntil = Instant.EPOCH;
        this.touchedAt = now;
    }
}
