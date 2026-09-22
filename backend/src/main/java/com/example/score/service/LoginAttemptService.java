package com.example.score.service;

import com.example.score.exception.BusinessException;
import com.example.score.service.ratelimit.AttemptRecord;
import com.example.score.service.ratelimit.AttemptStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;

/**
 * 登录失败次数限制，用于缓解针对登录接口的暴力破解。
 *
 * <p>计数通过 {@link AttemptStore} 存储，默认实现为进程内存储：
 * 服务重启后清零，多实例部署时各实例独立计数；替换为共享存储实现即可跨实例生效。</p>
 */
@Component
public class LoginAttemptService {

    private static final int MAX_TRACKED_KEYS = 1000;

    private final AttemptStore attemptStore;

    /** 同一账号在锁定窗口内允许的连续失败次数。 */
    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    /** 同一来源 IP 在锁定窗口内允许的连续失败次数。 */
    @Value("${security.login.ip-max-attempts:20}")
    private int ipMaxAttempts;

    /** 连续失败达到阈值后的锁定时长，同时也是失败计数的静默窗口。 */
    @Value("${security.login.lock-minutes:15}")
    private long lockMinutes;

    public LoginAttemptService(AttemptStore attemptStore) {
        this.attemptStore = attemptStore;
    }

    /**
     * 校验账号与来源 IP 是否处于锁定期，处于锁定状态时抛出 429。
     */
    public void checkNotLocked(String username, String ip) {
        Instant now = Instant.now();
        long remaining = Math.max(
                remainingSeconds(accountKey(username), now),
                remainingSeconds(ipKey(ip), now)
        );
        if (remaining > 0) {
            long minutes = Math.max(1, (remaining + 59) / 60);
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "登录失败次数过多，请 " + minutes + " 分钟后重试");
        }
    }

    /**
     * 记录一次登录失败。
     */
    public void recordFailure(String username, String ip) {
        Instant now = Instant.now();
        increase(accountKey(username), maxAttempts, now);
        increase(ipKey(ip), ipMaxAttempts, now);
        pruneIfNeeded(now);
    }

    /**
     * 登录成功后清空该账号与来源 IP 的失败计数。
     */
    public void reset(String username, String ip) {
        attemptStore.remove(accountKey(username));
        attemptStore.remove(ipKey(ip));
    }

    private void increase(String key, int limit, Instant now) {
        if (key == null) {
            return;
        }
        AttemptRecord record = attemptStore.get(key);
        if (record == null) {
            record = new AttemptRecord();
        }
        // 锁定期与静默窗口都已过期时重新计数，避免一次误输立刻再次锁定。
        if (record.isExpired(now, lockMinutes)) {
            record.reset(now);
        }
        record.setCount(record.getCount() + 1);
        record.setTouchedAt(now);
        if (record.getCount() >= limit) {
            record.setLockedUntil(now.plus(Duration.ofMinutes(lockMinutes)));
        }
        attemptStore.put(key, record);
    }

    private long remainingSeconds(String key, Instant now) {
        if (key == null) {
            return 0;
        }
        AttemptRecord record = attemptStore.get(key);
        if (record == null) {
            return 0;
        }
        if (record.getLockedUntil().isAfter(now)) {
            return Duration.between(now, record.getLockedUntil()).getSeconds();
        }
        if (record.isExpired(now, lockMinutes)) {
            attemptStore.remove(key);
        }
        return 0;
    }

    private void pruneIfNeeded(Instant now) {
        Map<String, AttemptRecord> entries = attemptStore.entries();
        if (entries.size() <= MAX_TRACKED_KEYS) {
            return;
        }
        Iterator<Map.Entry<String, AttemptRecord>> iterator = entries.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isExpired(now, lockMinutes)) {
                iterator.remove();
            }
        }
    }

    private String accountKey(String username) {
        return username == null ? null : "u:" + username;
    }

    private String ipKey(String ip) {
        return ip == null || ip.isBlank() ? null : "i:" + ip;
    }
}
