package com.example.score.service.ratelimit;

import java.util.Map;

/**
 * 失败计数存储抽象。
 *
 * <p>默认实现为进程内存储（{@link InMemoryAttemptStore}），
 * 多实例部署或需要重启后仍然生效时，可替换为 Redis 等共享存储实现。</p>
 */
public interface AttemptStore {

    AttemptRecord get(String key);

    void put(String key, AttemptRecord record);

    void remove(String key);

    /**
     * 当前全部记录，用于过期数据清理。
     */
    Map<String, AttemptRecord> entries();
}
