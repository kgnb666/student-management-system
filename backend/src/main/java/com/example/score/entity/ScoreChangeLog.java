package com.example.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩变更日志，只追加不修改。
 */
@Data
@TableName("score_change_log")
public class ScoreChangeLog {

    /** 操作类型。 */
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_UNLOCK = "UNLOCK";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scoreId;
    private Long courseId;
    private Long studentId;
    private Long operatorUserId;
    private String operatorName;
    private String operatorRole;
    private String action;
    private BigDecimal beforeUsualScore;
    private BigDecimal beforeExamScore;
    private BigDecimal beforeFinalScore;
    private BigDecimal afterUsualScore;
    private BigDecimal afterExamScore;
    private BigDecimal afterFinalScore;
    private String remark;
    private LocalDateTime createTime;
}
