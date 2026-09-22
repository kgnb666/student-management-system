package com.example.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("score")
public class Score {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long studentId;
    private BigDecimal usualScore;
    private BigDecimal examScore;
    private BigDecimal finalScore;
    /** 最后一次修改成绩的登录用户 id。 */
    private Long updateBy;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
