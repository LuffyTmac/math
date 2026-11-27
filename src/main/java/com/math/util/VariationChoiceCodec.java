package com.math.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.math.dto.ChoiceAnswer;

import java.util.ArrayList;
import java.util.List;

/**
 * 选择题答案的 JSON 编解码。
 *
 * <p>数据库中选择题的 answer 存为：
 * {"options":[["3","4"],["3","5"]],"correct":0}</p>
 *
 * <p>兼容早期单空格式：{"options":["12","14"],"correct":1}。</p>
 */
public final class VariationChoiceCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private VariationChoiceCodec() {
    }

    public static String encode(List<List<String>> options, int correct) {
        ChoiceAnswer payload = new ChoiceAnswer();
        payload.setOptions(options == null ? new ArrayList<>() : options);
        payload.setCorrect(correct);
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            return "{\"options\":[],\"correct\":0}";
        }
    }

    public static ChoiceAnswer decode(String json) {
        ChoiceAnswer payload = new ChoiceAnswer();
        payload.setOptions(new ArrayList<>());
        if (json == null || json.isEmpty()) {
            return payload;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode optionsNode = root.path("options");
            List<List<String>> options = new ArrayList<>();
            if (optionsNode.isArray()) {
                for (JsonNode opt : optionsNode) {
                    List<String> values = new ArrayList<>();
                    if (opt.isArray()) {
                        for (JsonNode v : opt) {
                            values.add(v.asText(""));
                        }
                    } else {
                        // 兼容单空旧格式：字符串选项
                        values.add(opt.asText(""));
                    }
                    options.add(values);
                }
            }
            payload.setOptions(options);
            payload.setCorrect(root.path("correct").asInt(0));
            return payload;
        } catch (Exception e) {
            return payload;
        }
    }

    public static boolean check(String json, Integer studentIndex) {
        if (studentIndex == null) {
            return false;
        }
        ChoiceAnswer payload = decode(json);
        return studentIndex >= 0
                && studentIndex < payload.getOptions().size()
                && studentIndex == payload.getCorrect();
    }

    /** 选项字母：A/B/C/D... */
    public static String letter(int index) {
        return String.valueOf((char) ('A' + index));
    }

    /** 选项展示文本：多个值用「、」连接 */
    public static String optionText(List<String> values) {
        return values == null ? "" : String.join("、", values);
    }
}
