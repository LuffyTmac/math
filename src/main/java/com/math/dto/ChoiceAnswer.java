package com.math.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 选择题答案：选项列表 + 正确选项下标（从 0 开始）。
 *
 * <p>每个选项是一组值，对应题干中的多个 □（单空时长度为 1）。</p>
 */
@Data
public class ChoiceAnswer {

    /** 选项：每个选项是各空的取值列表 */
    private List<List<String>> options = new ArrayList<>();

    /** 正确选项下标，0 开始 */
    private int correct;
}
