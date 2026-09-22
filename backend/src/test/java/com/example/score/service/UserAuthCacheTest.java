package com.example.score.service;

import com.example.score.entity.User;
import com.example.score.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 鉴权缓存：命中不查库、过期重新查库、写操作后立即失效。
 */
@ExtendWith(MockitoExtension.class)
class UserAuthCacheTest {

    @Mock
    private UserMapper userMapper;

    @Test
    @DisplayName("TTL 内命中缓存只查一次库")
    void shouldHitCacheWithinTtl() {
        UserAuthCache cache = cache(30_000);
        when(userMapper.selectById(1L)).thenReturn(user(1));

        User first = cache.get(1L);
        User second = cache.get(1L);

        assertThat(first.getTokenVersion()).isEqualTo(1);
        assertThat(second.getTokenVersion()).isEqualTo(1);
        verify(userMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("超过 TTL 后重新查库")
    void shouldReloadAfterTtl() {
        UserAuthCache cache = cache(0);
        when(userMapper.selectById(1L)).thenReturn(user(1));

        cache.get(1L);
        cache.get(1L);

        verify(userMapper, times(2)).selectById(1L);
    }

    @Test
    @DisplayName("主动失效后重新查库，保证改密/停用立即生效")
    void shouldReloadAfterEvict() {
        UserAuthCache cache = cache(30_000);
        when(userMapper.selectById(1L)).thenReturn(user(1));

        cache.get(1L);
        cache.evict(1L);
        cache.get(1L);

        verify(userMapper, times(2)).selectById(1L);
    }

    @Test
    @DisplayName("不存在的用户不缓存")
    void shouldNotCacheMissingUser() {
        UserAuthCache cache = cache(30_000);
        when(userMapper.selectById(9L)).thenReturn(null);

        assertThat(cache.get(9L)).isNull();
        assertThat(cache.get(9L)).isNull();

        verify(userMapper, times(2)).selectById(9L);
    }

    private UserAuthCache cache(long ttlMillis) {
        UserAuthCache cache = new UserAuthCache(userMapper);
        ReflectionTestUtils.setField(cache, "ttlMillis", ttlMillis);
        return cache;
    }

    private User user(int tokenVersion) {
        User user = new User();
        user.setId(1L);
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setTokenVersion(tokenVersion);
        user.setNeedChangePassword(0);
        return user;
    }
}
