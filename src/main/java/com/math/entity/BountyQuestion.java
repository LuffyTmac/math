package com.math.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 悬赏题目实体
 */
@Data
@TableName("bounty_question")
public class BountyQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目标题（列表展示用） */
    private String title;

    /** 题目内容（富文本 HTML） */
    private String content;

    /** 题目类型：CALC 计算题 / APPLICATION 应用题 / ORAL 口算题 */
    private String questionType;

    /** 悬赏星星数 */
    private Integer starCount;

    /** 状态：DRAFT 草稿 / PUBLISHED 已发布 */
    private String status;

    /** 提示与思路（富文本 HTML，admin 可选填写，默认不展示） */
    private String hintContent;

    /** 提示是否展示：0 隐藏 1 展示（admin 手动开启） */
    private Integer hintVisible;

    /** 创建人 id（admin） */
    private Long creatorId;

    /** 发布时间 */
    private LocalDateTime publishTime;

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
