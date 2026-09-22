package com.example.score.vo;

import java.util.List;

/**
 * 成绩 Excel 导入结果：成功/失败条数与逐行错误说明。
 */
public record ScoreImportResultVO(Integer successCount, Integer failCount, List<String> errors) {
}
