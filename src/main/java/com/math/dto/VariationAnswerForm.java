package com.math.dto;

import lombok.Data;

import java.util.List;

/**
 * 变式练习 · 学生作答（JSON body）
 */
@Data
public class VariationAnswerForm {

    private Long questionId;

    /** 填空题每个空的答案 */
    private List<String> answers;

    /** 判断题答案：TRUE / FALSE */
    private String judgeAnswer;

    /** 选择题：选中的选项下标（0 开始） */
    private Integer choiceAnswer;
}
