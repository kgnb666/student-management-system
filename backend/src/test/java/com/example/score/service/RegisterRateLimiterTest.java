package com.example.score.service;

import com.example.score.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 注册接口频率限制测试。
 */
class RegisterRateLimiterTest {

    @Test
    @DisplayName("窗口内超过上限后拒绝，其他 IP 不受影响")
    void shouldLimitByIp() {
        RegisterRateLimiter limiter = new RegisterRateLimiter();
        ReflectionTestUtils.setField(limiter, "maxPerHour", 2);
        ReflectionTestUtils.setField(limiter, "windowMinutes", 60L);

        assertThatCode(() -> limiter.checkAllowed("10.0.0.1")).doesNotThrowAnyException();
        assertThatCode(() -> limiter.checkAllowed("10.0.0.1")).doesNotThrowAnyException();
        assertThatThrownBy(() -> limiter.checkAllowed("10.0.0.1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("注册提交过于频繁");

        assertThatCode(() -> limiter.checkAllowed("10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("窗口过期后重新计数")
    void shouldResetAfterWindow() {
        RegisterRateLimiter limiter = new RegisterRateLimiter();
        ReflectionTestUtils.setField(limiter, "maxPerHour", 1);
        ReflectionTestUtils.setField(limiter, "windowMinutes", 0L);

        limiter.checkAllowed("10.0.0.3");

        assertThatCode(() -> limiter.checkAllowed("10.0.0.3")).doesNotThrowAnyException();
    }
}
