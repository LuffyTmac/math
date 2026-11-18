package com.math.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 悬赏答题实体
 */
@Data
@TableName("bounty_answer")
public class BountyAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目 id */
    private Long questionId;

    /** 答题人 id */
    private Long answerUserId;

    /** 答题内容（富文本 HTML：解题思路+答案） */
    private String answerContent;

    /** 提交时间 */
    private LocalDateTime submitTime;

    /** 评分星星数（admin 评分，未评分时为 null） */
    private Integer score;

    /** 评分状态：UNSCORED 未评分 / SCORED 已评分 */
    private String scoreStatus;

    /** 评分人 id（admin） */
    private Long scorerId;

    /** 评分时间 */
    private LocalDateTime scoreTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 纯文本预览（不入库，用于列表展示） */
    @TableField(exist = false)
    private String textPreview;

    @TableLogic
    private Integer deleted;
}
