package com.math.util;

import java.util.regex.Pattern;

/**
 * 富文本 XSS 基础清理工具
 * 移除 script 标签、事件属性、javascript: 协议等危险内容，保留安全的 HTML 格式标签。
 */
public final class XssUtil {

    private XssUtil() {}

    private static final Pattern SCRIPT_TAG = Pattern.compile(
            "<script[^>]*>[\\s\\S]*?</script>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_OPEN = Pattern.compile(
            "<script[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_CLOSE = Pattern.compile(
            "</script>", Pattern.CASE_INSENSITIVE);
    /** onXXX= 事件属性，如 onclick=, onload=, onerror= */
    private static final Pattern ON_EVENTS = Pattern.compile(
            "\\son\\w+\\s*=\\s*([\"'][^\"']*[\"']|[^\\s>]+)", Pattern.CASE_INSENSITIVE);
    /** javascript: 协议 */
    private static final Pattern JS_PROTOCOL = Pattern.compile(
            "(href|src)\\s*=\\s*[\"']\\s*javascript:", Pattern.CASE_INSENSITIVE);

    public static String clean(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        String s = html;
        s = SCRIPT_TAG.matcher(s).replaceAll("");
        s = SCRIPT_OPEN.matcher(s).replaceAll("");
        s = SCRIPT_CLOSE.matcher(s).replaceAll("");
        s = ON_EVENTS.matcher(s).replaceAll("");
        s = JS_PROTOCOL.matcher(s).replaceAll("$1=\"\"");
        return s;
    }
}
