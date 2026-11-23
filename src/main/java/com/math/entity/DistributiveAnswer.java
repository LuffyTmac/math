package com.math.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 乘法分配律学生算式实体
 * 不记录提交人身份，提交即展示
 */
@Data
@TableName("distributive_answer")
public class DistributiveAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属题目 id */
    private Long questionId;

    /** 学生提交的算式，如 3*(4+5) 或 3*4+3*5 */
    private String expression;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
