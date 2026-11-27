package com.math.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自定义书架 · 学生提交记录
 *
 * <p>保存左右两个书架的「层数 + 每层书本数」，同一个学生可以多次提交。</p>
 */
@Data
@TableName("shelf_answer")
public class ShelfAnswer {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 左书架层数 */
    private Integer leftLayers;

    /** 左书架每层书本数 */
    private Integer leftBooks;

    /** 右书架层数 */
    private Integer rightLayers;

    /** 右书架每层书本数 */
    private Integer rightBooks;

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
