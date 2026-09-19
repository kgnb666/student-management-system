package com.example.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("semester")
public class Semester {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String semesterName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer isCurrent;
    private LocalDateTime createTime;
}
