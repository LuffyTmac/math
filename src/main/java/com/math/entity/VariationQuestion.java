package com.math.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 变式练习 · 题目
 *
 * <p>type = FILL（填空，content 里用 □ 表示空格）或 JUDGE（判断，answer 为 TRUE/FALSE）。</p>
 */
@Data
@TableName("variation_question")
public class VariationQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** FILL / JUDGE */
    private String type;

    /** 题干（□ 表示需要学生填写的空） */
    private String content;

    /** 填空：JSON 数组；判断：TRUE / FALSE */
    private String answer;

    /** 排序，数字越小越靠前 */
    private Integer sortOrder;

    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
