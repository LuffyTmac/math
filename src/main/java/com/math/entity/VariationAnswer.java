package com.math.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 变式练习 · 学生作答记录
 */
@Data
@TableName("variation_answer")
public class VariationAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long questionId;

    private Long studentId;

    private String studentName;

    /** 填空：JSON 数组；判断：TRUE / FALSE */
    private String answer;

    /** 是否正确：1 正确 0 错误 */
    @TableField("is_correct")
    private Integer correct;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
