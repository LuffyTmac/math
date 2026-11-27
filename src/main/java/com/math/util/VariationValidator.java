package com.math.util;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 变式练习 · 题干与答案校验工具
 */
public final class VariationValidator {

    public static final int MAX_CONTENT = 500;

    private static final char BLANK = '□';

    private VariationValidator() {
    }

    /** 规范化：全角转半角、乘号/减号统一、去掉所有空白 */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        s = s.replace('×', '×')
             .replace('✕', '×')
             .replace('✖', '×')
             .replace('x', '×')
             .replace('X', '×')
             .replace('∗', '×')
             .replace('*', '×')
             .replace('−', '−')
             .replace('﹣', '−')
             .replace('–', '−')
             .replace('—', '−')
             .replace('-', '−')
             .replace('＋', '+')
             .replace('（', '(')
             .replace('）', ')')
             .replace('＝', '=');
        return s.replaceAll("[\\s\\u00A0\\u3000]+", "");
    }

    public static int blankCount(String content) {
        int count = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == BLANK) {
                count++;
            }
        }
        return count;
    }

    /**
     * 校验题目配置。
     *
     * @return 错误提示；校验通过返回 null
     */
    public static String validateQuestion(String type, String content, List<String> answers, String judgeAnswer,
                                          List<List<String>> options, Integer correctOption) {
        if (content == null || content.isEmpty()) {
            return "先写出题干吧～";
        }
        if (content.length() > MAX_CONTENT) {
            return "题干太长啦，精简一点哦～";
        }
        if (!content.matches("^[0-9+−×()=□]+$")) {
            return "题干只能用数字和 + − × ( ) = □ 哦～";
        }

        if ("FILL".equals(type)) {
            int blanks = blankCount(content);
            if (blanks < 1) {
                return "填空题至少放一个 □ 哦～";
            }
            if (answers == null || answers.size() != blanks) {
                return "空的数量和答案数量要一致哦～";
            }
            for (String a : answers) {
                String v = normalize(a);
                if (v.isEmpty()) {
                    return "每个空都要填写正确答案哦～";
                }
                if (!v.matches("^[0-9+−×()]+$")) {
                    return "答案只能用数字和 + − × ( ) 哦～";
                }
            }
            return null;
        }

        if ("JUDGE".equals(type)) {
            if (blankCount(content) > 0) {
                return "判断题不需要 □ 哦～";
            }
            if (!"TRUE".equals(judgeAnswer) && !"FALSE".equals(judgeAnswer)) {
                return "请选择判断题的正确答案～";
            }
            return null;
        }

        if ("CHOICE".equals(type)) {
            int blanks = blankCount(content);
            if (blanks < 1) {
                return "选择题题干至少要放一个 □ 哦～";
            }
            if (blanks > 6) {
                return "选择题的空太多了，最多 6 个哦～";
            }
            if (options == null || options.size() < 2 || options.size() > 6) {
                return "选择题需要 2 ~ 6 个选项哦～";
            }
            if (correctOption == null || correctOption < 0 || correctOption >= options.size()) {
                return "请选中一个正确选项～";
            }
            Set<String> seen = new HashSet<>();
            for (List<String> option : options) {
                if (option == null || option.size() != blanks) {
                    return "每个选项都要填满 " + blanks + " 个空哦～";
                }
                StringBuilder key = new StringBuilder();
                for (String o : option) {
                    String v = normalize(o);
                    if (v.isEmpty()) {
                        return "选项里的每个空都要填写哦～";
                    }
                    if (!v.matches("^[0-9+−×()]+$")) {
                        return "选项只能用数字和 + − × ( ) 哦～";
                    }
                    key.append(v).append('|');
                }
                if (!seen.add(key.toString())) {
                    return "选项不能重复哦～";
                }
            }
            return null;
        }

        return "请先选择题目类型～";
    }

    /** 判断填空作答是否正确 */
    public static boolean checkFill(List<String> correct, List<String> student) {
        if (correct == null || student == null || correct.size() != student.size()) {
            return false;
        }
        for (int i = 0; i < correct.size(); i++) {
            if (!sameToken(correct.get(i), student.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** 判断判断题作答是否正确 */
    public static boolean checkJudge(String correct, String student) {
        return correct != null && correct.equalsIgnoreCase(student == null ? "" : student.trim());
    }

    /** 单个答案比较：规范化后相同，或数值相等 */
    private static boolean sameToken(String a, String b) {
        String x = normalizeForCompare(a);
        String y = normalizeForCompare(b);
        if (x.equals(y)) {
            return true;
        }
        try {
            return new BigDecimal(x).compareTo(new BigDecimal(y)) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** 比较用：× → *，− → -，去空白 */
    public static String normalizeForCompare(String raw) {
        return normalize(raw).replace('×', '*').replace('−', '-');
    }

    public static String display(String raw) {
        return normalize(raw);
    }

    public static String judgeText(String answer) {
        return "TRUE".equalsIgnoreCase(answer) ? "正确" : "错误";
    }

    public static List<String> asList(List<String> list) {
        return list;
    }
}
