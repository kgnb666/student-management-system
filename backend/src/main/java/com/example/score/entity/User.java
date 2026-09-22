package com.example.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @JsonIgnore
    private String password;
    private String role;
    private String realName;
    private Integer status;
    /**
     * token 版本号。修改密码或变更账号状态时自增，使旧 token 立即失效。
     */
    private Integer tokenVersion;
    /**
     * 是否需要强制修改密码：管理员新建账号或重置密码后置 1，本人改密后置 0。
     */
    private Integer needChangePassword;
    private LocalDateTime createTime;
}
