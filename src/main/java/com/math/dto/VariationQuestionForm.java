package com.math.dto;

import lombok.Data;

import java.util.List;

/**
 * 变式练习 · 新增/编辑题目表单（JSON body）
 */
@Data
public class VariationQuestionForm {

    private Long id;

    /** FILL / JUDGE */
    private String type;

    /** 题干：填空用 □ 表示空格 */
    private String content;

    /** 填空题每个空的答案（数量与 □ 一致） */
    private List<String> answers;

    /** 判断题答案：TRUE / FALSE */
    private String judgeAnswer;

    /** 选择题选项：每个选项是各空的取值列表（2 ~ 6 个选项） */
    private List<List<String>> options;

    /** 选择题正确选项下标（0 开始） */
    private Integer correctOption;

    private Integer sortOrder;
}
