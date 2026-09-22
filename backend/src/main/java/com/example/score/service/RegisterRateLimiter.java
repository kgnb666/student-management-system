package com.example.score.service;

import com.example.score.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册接口的频率限制：同一来源 IP 在滑动窗口内最多提交若干次。
 *
 * <p>与登录失败计数一致，这里也是进程内实现：服务重启后清零，多实例之间不共享。</p>
 */
@Component
public class RegisterRateLimiter {

    private final Map<String, Deque<Instant>> submissions = new ConcurrentHashMap<>();

    /** 窗口内允许的注册次数。 */
    @Value("${security.register.max-per-hour:5}")
    private int maxPerHour;

    /** 计数窗口（分钟），默认 60 分钟。 */
    @Value("${security.register.window-minutes:60}")
    private long windowMinutes;

    public void checkAllowed(String ip) {
        if (ip == null || ip.isBlank()) {
            return;
        }
        Instant now = Instant.now();
        Deque<Instant> timestamps = submissions.computeIfAbsent(ip, key -> new ArrayDeque<>());
        synchronized (timestamps) {
            removeExpired(timestamps, now);
            if (timestamps.size() >= maxPerHour) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                        "注册提交过于频繁，请稍后再试");
            }
            timestamps.addLast(now);
        }
        pruneIfNeeded(now);
    }

    private void removeExpired(Deque<Instant> timestamps, Instant now) {
        Instant threshold = now.minus(Duration.ofMinutes(windowMinutes));
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
            timestamps.pollFirst();
        }
    }

    private void pruneIfNeeded(Instant now) {
        if (submissions.size() <= 1000) {
            return;
        }
        submissions.entrySet().removeIf(entry -> {
            Deque<Instant> timestamps = entry.getValue();
            synchronized (timestamps) {
                removeExpired(timestamps, now);
                return timestamps.isEmpty();
            }
        });
    }
}
