package com.example.score.vo;

/**
 * 注册页面使用的班级选项，只包含班级 id 与名称，避免向匿名用户暴露班主任等信息。
 */
public record ClassOptionVO(Long id, String className) {
}
