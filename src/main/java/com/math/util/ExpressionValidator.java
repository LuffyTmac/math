package com.math.util;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 乘法分配律 · 学生算式校验工具
 *
 * <p>只放行两种写法（规范化之后）：</p>
 * <ul>
 *   <li>分配式：{@code a*(b+c)} / {@code a*(b-c)}，如 {@code 3*(4+5)}</li>
 *   <li>展开式：{@code a*b+a*c} / {@code a*b-a*c}，如 {@code 3*4+3*5}</li>
 * </ul>
 *
 * <p>规范化：全角转半角（NFKC）、{@code × x X} 转 {@code *}、各种减号统一、去掉所有空白。</p>
 *
 * <p>服务端最终校验以本类为准，前端 {@code distributive-learn.js} 保持同一套规则与提示文案。</p>
 */
public final class ExpressionValidator {

    /** 规范化后允许的最大长度 */
    public static final int MAX_LENGTH = 200;

    private static final Pattern ALLOWED = Pattern.compile("^[0-9+\\-*()]+$");
    private static final Pattern DISTRIBUTE = Pattern.compile("^\\d+\\*\\(\\d+[+-]\\d+\\)$");
    private static final Pattern EXPAND = Pattern.compile("^(\\d+)\\*(\\d+)([+-])(\\d+)\\*(\\d+)$");
    private static final Pattern INNER = Pattern.compile("\\(([^()]*)\\)");
    private static final Pattern INNER_OK = Pattern.compile("^\\d+[+-]\\d+$");

    private ExpressionValidator() {
    }

    /**
     * 校验并规范化学生提交的算式。
     *
     * @param raw 学生原始输入（可能含全角符号、空格）
     * @return 校验结果，{@link Result#isValid()} 为 false 时 {@link Result#getMessage()} 是给孩子的提示
     */
    public static Result validate(String raw) {
        String s = normalize(raw);
        if (s.isEmpty()) {
            return Result.fail("先写一个算式吧～");
        }
        if (s.length() > MAX_LENGTH) {
            return Result.fail("算式太长啦，简单一点哦～");
        }
        if (!ALLOWED.matcher(s).matches()) {
            return Result.fail("只能用数字和 + - × ( ) 哦～");
        }
        if (s.indexOf('*') < 0) {
            return Result.fail("分配律里要出现乘号 × 哦～");
        }
        if (!bracketsBalanced(s)) {
            return Result.fail("括号要成对出现，像这样：(4+5)");
        }
        // 形式一：a*(b+c)
        if (DISTRIBUTE.matcher(s).matches()) {
            return Result.ok(s);
        }
        // 形式二：a*b+a*c
        Matcher m = EXPAND.matcher(s);
        if (m.matches()) {
            int a1 = Integer.parseInt(m.group(1));
            int b1 = Integer.parseInt(m.group(2));
            int a2 = Integer.parseInt(m.group(4));
            int b2 = Integer.parseInt(m.group(5));
            if (a1 != a2 && b1 != b2) {
                return Result.fail("两个乘法要有一个相同的数，比如 3×4＋3×5 里的 3 哦～");
            }
            return Result.ok(s);
        }
        // 括号里的内容不合法：3*(4+5+6)、3*()、3*(4*5) 等
        Matcher inner = INNER.matcher(s);
        if (inner.find() && !INNER_OK.matcher(inner.group(1)).matches()) {
            return Result.fail("括号里写成「4+5」这样就好啦～");
        }
        return Result.fail("试试写成 3×(4+5) 或 3×4＋3×5 吧～");
    }

    /**
     * 规范化：全角转半角、乘号统一、减号统一、去掉所有空白。
     */
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
        // 普通空格、不换行空格、全角空格一律去掉
        return s.replaceAll("[\\s\\u00A0\\u3000]+", "");
    }

    /** 括号是否配对且不出现空括号、负深度 */
    private static boolean bracketsBalanced(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth < 0) {
                    return false;
                }
            }
        }
        return depth == 0;
    }

    /** 校验结果 */
    public static final class Result {
        private final boolean valid;
        private final String normalized;
        private final String message;

        private Result(boolean valid, String normalized, String message) {
            this.valid = valid;
            this.normalized = normalized;
            this.message = message;
        }

        static Result ok(String normalized) {
            return new Result(true, normalized, null);
        }

        static Result fail(String message) {
            return new Result(false, null, message);
        }

        public boolean isValid() {
            return valid;
        }

        /** 规范化后的算式（仅校验通过时有值） */
        public String getNormalized() {
            return normalized;
        }

        /** 给孩子看的提示（仅校验失败时有值） */
        public String getMessage() {
            return message;
        }
    }
}
