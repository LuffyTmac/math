package com.math.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 乘法分配律题目实体
 */
@Data
@TableName("distributive_question")
public class DistributiveQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目名称（卡片标题） */
    private String title;

    /** 图片链接（卡片主图） */
    private String imageUrl;

    /** 详细说明（富文本 HTML） */
    private String description;

    /** 状态：DRAFT 草稿 / PUBLISHED 已发布 */
    private String status;

    /** 排序号（越小越靠前，admin 可调整卡片顺序） */
    private Integer sortOrder;

    /** 创建人 id（admin / teacher） */
    private Long creatorId;

    /** 发布时间 */
    private LocalDateTime publishTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 该题已提交的算式数量（不入库，用于列表展示） */
    @TableField(exist = false)
    private Integer answerCount;

    @TableLogic
    private Integer deleted;
}
