package com.example.score.service;

import com.example.score.entity.User;
import com.example.score.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 鉴权用的用户信息短时缓存，避免每个请求都查一次 sys_user。
 *
 * <p>缓存只保存鉴权相关字段（状态、token 版本、角色、是否需改密），
 * 并对外返回副本；账号状态变更、改密、重置密码等写操作必须调用 {@link #evict(Long)}
 * 保证「旧 token 立即失效」的语义不回退。</p>
 */
@Component
public class UserAuthCache {

    private final UserMapper userMapper;
    private final Map<Long, Entry> cache = new ConcurrentHashMap<>();

    /** 缓存有效期（毫秒）。 */
    @Value("${security.auth-cache-millis:30000}")
    private long ttlMillis;

    public UserAuthCache(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User get(Long userId) {
        if (userId == null) {
            return null;
        }
        Entry entry = cache.get(userId);
        if (entry != null && !entry.expired(ttlMillis)) {
            return entry.toUser();
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            cache.remove(userId);
            return null;
        }
        cache.put(userId, Entry.of(user));
        return user;
    }

    /**
     * 账号信息发生写操作后立即清除缓存。
     */
    public void evict(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    private static final class Entry {
        private final String role;
        private final Integer status;
        private final Integer tokenVersion;
        private final Integer needChangePassword;
        private final long createdAt;

        private Entry(User user, long createdAt) {
            this.role = user.getRole();
            this.status = user.getStatus();
            this.tokenVersion = user.getTokenVersion();
            this.needChangePassword = user.getNeedChangePassword();
            this.createdAt = createdAt;
        }

        private static Entry of(User user) {
            return new Entry(user, System.currentTimeMillis());
        }

        private boolean expired(long ttlMillis) {
            return System.currentTimeMillis() - createdAt >= ttlMillis;
        }

        private User toUser() {
            User user = new User();
            user.setRole(role);
            user.setStatus(status);
            user.setTokenVersion(tokenVersion);
            user.setNeedChangePassword(needChangePassword);
            return user;
        }
    }
}
