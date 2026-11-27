package com.math.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自定义等式 · 学生提交记录
 *
 * <p>错误的等式也会入库，用 {@code correct}（数据库列 {@code is_correct}）标记。</p>
 */
@Data
@TableName("equation_answer")
public class EquationAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生提交的等式（乘号用 ×，已去空格） */
    private String expression;

    /** 是否正确：1 正确 0 错误 */
    @TableField("is_correct")
    private Integer correct;

    /** 提交学生 id */
    private Long studentId;

    /** 提交学生姓名（昵称优先） */
    private String studentName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
