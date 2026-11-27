package com.math.util;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自定义等式 · 校验工具
 *
 * <p>正确的格式（规范化之后）：{@code A×(B+C)=A×B+A×C}，也允许减号版本
 * {@code A×(B-C)=A×B-A×C}，以及右侧两个乘积交换顺序。</p>
 *
 * <p>格式不合法或等号两边不成立的都算“错误”，但都会照常入库，
 * 用 {@code is_correct} 字段标记，方便后续展示。</p>
 */
public final class EquationValidator {

    /** 规范化后允许的最大长度 */
    public static final int MAX_LENGTH = 200;

    private static final Pattern ALLOWED = Pattern.compile("^[0-9+\\-*()=]+$");
    private static final Pattern SHAPE = Pattern.compile(
            "^(\\d+)\\*\\((\\d+)([+\\-])(\\d+)\\)=(\\d+)\\*(\\d+)([+\\-])(\\d+)\\*(\\d+)$");

    private EquationValidator() {
    }

    /**
     * 校验学生提交的等式。
     *
     * @param raw 学生原始输入（可能含全角符号、空格）
     * @return 结果：{@link Result#getDisplay()} 是拿去入库的展示文本，{@link Result#isCorrect()} 表示等式是否正确
     */
    public static Result validate(String raw) {
        String s = normalize(raw);
        if (s.isEmpty()) {
            return new Result("", false, "先写一个等式吧～");
        }
        if (s.length() > MAX_LENGTH) {
            return new Result(s, false, "等式太长啦，简单一点哦～");
        }
        if (!ALLOWED.matcher(s).matches()) {
            return new Result(toDisplay(s), false, "只能用数字和 + − × ( ) = 哦～");
        }
        int eq = s.indexOf('=');
        if (eq < 0) {
            return new Result(toDisplay(s), false, "等号「=」不能少哦～");
        }
        if (s.indexOf('=', eq + 1) >= 0) {
            return new Result(toDisplay(s), false, "等号只能有一个哦～");
        }

        Matcher m = SHAPE.matcher(s);
        if (!m.matches()) {
            return new Result(toDisplay(s), false, "格式要写成 A×(B+C)=A×B+A×C 哦～");
        }

        int a = Integer.parseInt(m.group(1));
        int b = Integer.parseInt(m.group(2));
        String op1 = m.group(3);
        int c = Integer.parseInt(m.group(4));
        int d = Integer.parseInt(m.group(5));
        int e = Integer.parseInt(m.group(6));
        String op2 = m.group(7);
        int f = Integer.parseInt(m.group(8));
        int g = Integer.parseInt(m.group(9));

        // 右侧是 A×B 与 A×C：第一个因数必须都等于 A，后两个因数分别是 B、C（顺序可交换）
        boolean same = d == a
                && f == a
                && op1.equals(op2)
                && ((e == b && g == c) || (e == c && g == b));

        String display = a + "×(" + b + op1 + c + ")=" + d + "×" + e + op2 + f + "×" + g;
        if (!same) {
            return new Result(display, false, "格式对啦，但等号两边要符合 A×(B+C)=A×B+A×C 哦～");
        }
        return new Result(display, true, "太棒了！这个等式成立～");
    }

    /** 规范化：全角转半角、乘号/减号统一、去掉所有空白 */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        s = s.replace('×', '*')
             .replace('✕', '*')
             .replace('✖', '*')
             .replace('x', '*')
             .replace('X', '*')
             .replace('∗', '*')
             .replace('−', '-')
             .replace('﹣', '-')
             .replace('–', '-')
             .replace('—', '-')
             .replace('＋', '+')
             .replace('（', '(')
             .replace('）', ')');
        return s.replaceAll("[\\s\\u00A0\\u3000]+", "");
    }

    /** 转成给孩子看的展示文本：乘号 ×、减号 − */
    public static String toDisplay(String s) {
        return s == null ? "" : s.replace('*', '×').replace('-', '−');
    }

    /** 校验结果 */
    public static final class Result {
        private final String display;
        private final boolean correct;
        private final String message;

        public Result(String display, boolean correct, String message) {
            this.display = display;
            this.correct = correct;
            this.message = message;
        }

        public String getDisplay() {
            return display;
        }

        public boolean isCorrect() {
            return correct;
        }

        public String getMessage() {
            return message;
        }
    }
}
