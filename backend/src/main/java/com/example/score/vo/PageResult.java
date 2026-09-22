package com.example.score.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

/**
 * 分页查询结果。仅在请求显式传入 page 参数时返回，未传时接口仍返回原数组结构。
 */
public record PageResult<T>(List<T> records, long total, long current, long size) {

    private static final long DEFAULT_SIZE = 10;
    private static final long MAX_SIZE = 100;

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public static long normalizeCurrent(Long page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static long normalizeSize(Long size) {
        if (size == null || size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
