package com.example.score.service.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内失败计数存储。服务重启后清零，多实例之间不共享。
 *
 * <p>如需接入 Redis 等共享存储，提供一个新的 {@link AttemptStore} 实现并标注 {@code @Primary} 即可。</p>
 */
@Component
public class InMemoryAttemptStore implements AttemptStore {

    private final Map<String, AttemptRecord> records = new ConcurrentHashMap<>();

    @Override
    public AttemptRecord get(String key) {
        return key == null ? null : records.get(key);
    }

    @Override
    public void put(String key, AttemptRecord record) {
        if (key != null && record != null) {
            records.put(key, record);
        }
    }

    @Override
    public void remove(String key) {
        if (key != null) {
            records.remove(key);
        }
    }

    @Override
    public Map<String, AttemptRecord> entries() {
        return records;
    }
}
