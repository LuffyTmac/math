package com.math.dto;

import lombok.Data;

/**
 * 乘法分配律 · 新增/编辑题目的表单 DTO
 *
 * <p>用来替代 add/edit 接口上的一长串 {@code @RequestParam}，由 Spring MVC 自动按字段名绑定：
 * 表单里的 {@code title / imageUrl / description / sortOrder / saveAction / id} 会直接落到这些属性上。</p>
 *
 * <p>校验仍由 {@code AdminDistributiveController} 里的校验方法完成（项目未引入 validation starter）。</p>
 */
@Data
public class DistributiveQuestionForm {

    /** 编辑时携带的题目 id；新增时为空 */
    private Long id;

    /** 题目名称（必填） */
    private String title;

    /** 图片链接（选填，站内 /uploads/... 或 http(s)://） */
    private String imageUrl;

    /** 详细说明（富文本，选填） */
    private String description;

    /** 排序号（选填，为空时自动取当前最大值 +1） */
    private Integer sortOrder;

    /** 按钮动作：DRAFT 保存草稿 / PUBLISH 发布 */
    private String saveAction;

    /** 是否点击了「发布」按钮 */
    public boolean isPublish() {
        return "PUBLISH".equals(saveAction);
    }
}
