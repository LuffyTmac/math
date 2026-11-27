package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.math.dto.ChoiceAnswer;
import com.math.dto.IdForm;
import com.math.dto.VariationQuestionForm;
import com.math.entity.SysUser;
import com.math.entity.VariationQuestion;
import com.math.entity.VariationAnswer;
import com.math.mapper.SysUserMapper;
import com.math.mapper.VariationAnswerMapper;
import com.math.mapper.VariationQuestionMapper;
import com.math.util.VariationChoiceCodec;
import com.math.util.VariationValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 变式练习 · 教师/管理员后台
 *
 * <p>POST 接口统一用 {@link RequestBody} 接收 JSON。</p>
 */
@Controller
@RequestMapping("/variation/admin")
public class VariationAdminController {

    private static final int PAGE_SIZE = 10;

    @Autowired
    private VariationQuestionMapper questionMapper;

    @Autowired
    private VariationAnswerMapper answerMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "1") int page, Model model) {
        long total = questionMapper.selectCount(new LambdaQueryWrapper<>());
        int pages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.min(Math.max(1, page), pages);
        int offset = (current - 1) * PAGE_SIZE;

        List<VariationQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<VariationQuestion>()
                        .orderByAsc(VariationQuestion::getSortOrder)
                        .orderByDesc(VariationQuestion::getId)
                        .last("LIMIT " + offset + "," + PAGE_SIZE));

        // 每道题的作答人数 / 正确人数 / 准确率
        Map<Long, int[]> stats = new HashMap<>();
        if (!questions.isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (VariationQuestion q : questions) {
                ids.add(q.getId());
            }
            List<VariationAnswer> answers = answerMapper.selectList(
                    new LambdaQueryWrapper<VariationAnswer>()
                            .in(VariationAnswer::getQuestionId, ids));
            for (VariationAnswer a : answers) {
                int[] st = stats.get(a.getQuestionId());
                if (st == null) {
                    st = new int[]{0, 0};
                    stats.put(a.getQuestionId(), st);
                }
                st[0]++;
                if (a.getCorrect() != null && a.getCorrect() == 1) {
                    st[1]++;
                }
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (VariationQuestion q : questions) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", q.getId());
            row.put("type", q.getType());
            row.put("typeText", "FILL".equals(q.getType()) ? "填空"
                    : ("JUDGE".equals(q.getType()) ? "判断" : "选择"));
            row.put("content", q.getContent());
            row.put("answerText", answerText(q));
            row.put("sortOrder", q.getSortOrder());
            row.put("createTime", q.getCreateTime());

            int[] st = stats.get(q.getId());
            int qTotal = st == null ? 0 : st[0];
            int qCorrect = st == null ? 0 : st[1];
            row.put("answerCount", qTotal);
            row.put("accuracy", qTotal == 0 ? "0.00" : String.format("%.2f", qCorrect * 100.0 / qTotal));
            rows.add(row);
        }

        model.addAttribute("rows", rows);
        model.addAttribute("page", current);
        model.addAttribute("pages", pages);
        model.addAttribute("total", total);
        model.addAttribute("hasPrev", current > 1);
        model.addAttribute("hasNext", current < pages);
        return "variation/admin_list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("question", null);
        model.addAttribute("answersJson", "[]");
        model.addAttribute("optionsJson", "[]");
        model.addAttribute("correctOption", 0);
        return "variation/admin_edit";
    }

    @GetMapping("/edit")
    public String editForm(@RequestParam Long id, Model model) {
        VariationQuestion question = questionMapper.selectById(id);
        if (question == null) {
            return "redirect:/variation/admin/list";
        }
        List<String> answers = parseAnswers(question.getAnswer());
        String optionsJson = "[]";
        int correctOption = 0;
        if ("CHOICE".equals(question.getType())) {
            ChoiceAnswer choice = VariationChoiceCodec.decode(question.getAnswer());
            optionsJson = toJson(choice.getOptions());
            correctOption = choice.getCorrect();
        }
        model.addAttribute("question", question);
        model.addAttribute("answersJson", toJson(answers));
        model.addAttribute("optionsJson", optionsJson);
        model.addAttribute("correctOption", correctOption);
        return "variation/admin_edit";
    }

    @PostMapping("/save")
    @ResponseBody
    public Map<String, Object> save(@RequestBody VariationQuestionForm form, Principal principal) {
        String type = form.getType();
        String content = VariationValidator.normalize(form.getContent());
        List<String> answers = new ArrayList<>();
        if (form.getAnswers() != null) {
            for (String a : form.getAnswers()) {
                answers.add(VariationValidator.normalize(a));
            }
        }
        String judgeAnswer = form.getJudgeAnswer();

        List<List<String>> options = new ArrayList<>();
        if (form.getOptions() != null) {
            for (List<String> o : form.getOptions()) {
                List<String> values = new ArrayList<>();
                if (o != null) {
                    for (String v : o) {
                        values.add(VariationValidator.normalize(v));
                    }
                }
                options.add(values);
            }
        }
        Integer correctOption = form.getCorrectOption();

        String error = VariationValidator.validateQuestion(type, content, answers, judgeAnswer,
                options, correctOption);
        if (error != null) {
            return fail(error);
        }

        String storedAnswer;
        if ("FILL".equals(type)) {
            storedAnswer = toJson(answers);
        } else if ("CHOICE".equals(type)) {
            storedAnswer = VariationChoiceCodec.encode(options, correctOption == null ? 0 : correctOption);
        } else {
            storedAnswer = "TRUE".equals(judgeAnswer) ? "TRUE" : "FALSE";
        }

        SysUser me = currentUser(principal);
        VariationQuestion question;
        if (form.getId() != null) {
            question = questionMapper.selectById(form.getId());
            if (question == null) {
                return fail("题目不存在，刷新后再试～");
            }
        } else {
            question = new VariationQuestion();
            question.setCreatorId(me == null ? null : me.getId());
        }
        question.setType(type);
        question.setContent(content);
        question.setAnswer(storedAnswer);
        question.setSortOrder(form.getSortOrder() == null ? 0 : form.getSortOrder());

        if (question.getId() == null) {
            questionMapper.insert(question);
        } else {
            questionMapper.updateById(question);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", question.getId());
        result.put("message", "✅ 已保存");
        return result;
    }

    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> delete(@RequestBody IdForm form) {
        if (form.getId() == null) {
            return fail("缺少题目 id");
        }
        questionMapper.deleteById(form.getId());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "🗑 已删除");
        return result;
    }


    /** 题目详情：作答人数、准确率、答对/答错的学生名单 */
    @GetMapping("/detail")
    public String detail(@RequestParam Long id, Model model) {
        VariationQuestion question = questionMapper.selectById(id);
        if (question == null) {
            return "redirect:/variation/admin/list";
        }
        List<VariationAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<VariationAnswer>()
                        .eq(VariationAnswer::getQuestionId, id)
                        .orderByDesc(VariationAnswer::getId));

        List<Map<String, Object>> correctList = new ArrayList<>();
        List<Map<String, Object>> wrongList = new ArrayList<>();
        for (VariationAnswer a : answers) {
            Map<String, Object> item = new HashMap<>();
            item.put("studentName", a.getStudentName() == null ? "学生" : a.getStudentName());
            item.put("answerText", studentAnswerText(a, question));
            item.put("time", a.getCreateTime() == null ? "" : a.getCreateTime().toString().replace('T', ' '));
            if (a.getCorrect() != null && a.getCorrect() == 1) {
                correctList.add(item);
            } else {
                wrongList.add(item);
            }
        }
        int total = answers.size();
        int correct = correctList.size();

        // 选择题：选项逐行展示 + 正确选项
        List<String> choiceOptionTexts = new ArrayList<>();
        int choiceCorrectIndex = -1;
        String choiceCorrectText = "";
        if ("CHOICE".equals(question.getType())) {
            ChoiceAnswer choice = VariationChoiceCodec.decode(question.getAnswer());
            for (List<String> values : choice.getOptions()) {
                choiceOptionTexts.add(VariationChoiceCodec.optionText(values));
            }
            choiceCorrectIndex = choice.getCorrect();
            if (choiceCorrectIndex >= 0 && choiceCorrectIndex < choiceOptionTexts.size()) {
                choiceCorrectText = VariationChoiceCodec.letter(choiceCorrectIndex)
                        + ". " + choiceOptionTexts.get(choiceCorrectIndex);
            }
        }

        model.addAttribute("choiceOptions", choiceOptionTexts);
        model.addAttribute("choiceCorrectIndex", choiceCorrectIndex);
        model.addAttribute("choiceCorrectText", choiceCorrectText);
        model.addAttribute("question", question);
        model.addAttribute("answerText", answerText(question));
        model.addAttribute("total", total);
        model.addAttribute("correctCount", correct);
        model.addAttribute("wrongCount", wrongList.size());
        model.addAttribute("accuracy", total == 0 ? "0.00" : String.format("%.2f", correct * 100.0 / total));
        model.addAttribute("correctList", correctList);
        model.addAttribute("wrongList", wrongList);
        return "variation/admin_detail";
    }

    private String studentAnswerText(VariationAnswer a, VariationQuestion q) {
        if (a.getAnswer() == null || a.getAnswer().isEmpty()) {
            return "";
        }
        if ("CHOICE".equals(q.getType())) {
            try {
                int idx = Integer.parseInt(a.getAnswer().trim());
                ChoiceAnswer choice = VariationChoiceCodec.decode(q.getAnswer());
                if (idx >= 0 && idx < choice.getOptions().size()) {
                    return VariationChoiceCodec.letter(idx) + ". "
                            + VariationChoiceCodec.optionText(choice.getOptions().get(idx));
                }
            } catch (NumberFormatException ignored) {
                // 兼容历史数据
            }
            return a.getAnswer();
        }
        if (a.getAnswer().startsWith("[")) {
            return String.join("、", parseAnswers(a.getAnswer()));
        }
        return VariationValidator.judgeText(a.getAnswer());
    }
    private String answerText(VariationQuestion q) {
        if ("JUDGE".equals(q.getType())) {
            return VariationValidator.judgeText(q.getAnswer());
        }
        if ("CHOICE".equals(q.getType())) {
            ChoiceAnswer choice = VariationChoiceCodec.decode(q.getAnswer());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < choice.getOptions().size(); i++) {
                if (i > 0) {
                    sb.append("  ");
                }
                sb.append(VariationChoiceCodec.letter(i)).append(".")
                  .append(VariationChoiceCodec.optionText(choice.getOptions().get(i)));
            }
            sb.append("（").append(VariationChoiceCodec.letter(choice.getCorrect())).append(" 正确）");
            return sb.toString();
        }
        List<String> list = parseAnswers(q.getAnswer());
        return String.join("、", list);
    }

    private List<String> parseAnswers(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String toJson(Object list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    private SysUser currentUser(Principal principal) {
        if (principal == null) {
            return null;
        }
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, principal.getName()));
    }

    private Map<String, Object> fail(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", message);
        return result;
    }
}
