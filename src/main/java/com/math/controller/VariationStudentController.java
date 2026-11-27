package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.math.dto.ChoiceAnswer;
import com.math.dto.VariationAnswerForm;
import com.math.entity.SysUser;
import com.math.entity.VariationAnswer;
import com.math.entity.VariationQuestion;
import com.math.mapper.SysUserMapper;
import com.math.mapper.VariationAnswerMapper;
import com.math.mapper.VariationQuestionMapper;
import com.math.util.VariationChoiceCodec;
import com.math.util.VariationValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 变式练习 · 学生端
 *
 * <p>左侧展示最新的填空题，右侧展示最新的判断题，各 3 道。</p>
 */
@Controller
@RequestMapping("/variation")
public class VariationStudentController {

    private static final int COLUMN_SIZE = 3;

    @Autowired
    private VariationQuestionMapper questionMapper;

    @Autowired
    private VariationAnswerMapper answerMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/learn")
    public String learn(Model model, Principal principal) {
        List<VariationQuestion> fill = questionMapper.selectList(
                new LambdaQueryWrapper<VariationQuestion>()
                        .eq(VariationQuestion::getType, "FILL")
                        .orderByAsc(VariationQuestion::getSortOrder)
                        .orderByDesc(VariationQuestion::getId)
                        .last("LIMIT " + COLUMN_SIZE));
        List<VariationQuestion> judge = questionMapper.selectList(
                new LambdaQueryWrapper<VariationQuestion>()
                        .eq(VariationQuestion::getType, "JUDGE")
                        .orderByAsc(VariationQuestion::getSortOrder)
                        .orderByDesc(VariationQuestion::getId)
                        .last("LIMIT " + COLUMN_SIZE));
        List<VariationQuestion> choice = questionMapper.selectList(
                new LambdaQueryWrapper<VariationQuestion>()
                        .eq(VariationQuestion::getType, "CHOICE")
                        .orderByAsc(VariationQuestion::getSortOrder)
                        .orderByDesc(VariationQuestion::getId)
                        .last("LIMIT " + COLUMN_SIZE));
        // 该学生已提交过的题目 id，前端据此显示「已提交答案」
        SysUser me = currentUser(principal);
        Set<Long> submittedIds = new HashSet<>();
        Map<Long, String> myAnswers = new HashMap<>();
        if (me != null) {
            List<VariationAnswer> mine = answerMapper.selectList(
                    new LambdaQueryWrapper<VariationAnswer>()
                            .eq(VariationAnswer::getStudentId, me.getId())
                            .orderByDesc(VariationAnswer::getId));
            Map<Long, VariationQuestion> questionOf = new HashMap<>();
            for (VariationQuestion q : fill) {
                questionOf.put(q.getId(), q);
            }
            for (VariationQuestion q : judge) {
                questionOf.put(q.getId(), q);
            }
            for (VariationQuestion q : choice) {
                questionOf.put(q.getId(), q);
            }
            for (VariationAnswer a : mine) {
                if (myAnswers.containsKey(a.getQuestionId())) {
                    continue;
                }
                submittedIds.add(a.getQuestionId());
                VariationQuestion q = questionOf.get(a.getQuestionId());
                if (q == null) {
                    continue;
                }
                if ("FILL".equals(q.getType())) {
                    myAnswers.put(a.getQuestionId(), String.join("、", parseAnswers(a.getAnswer())));
                } else if ("JUDGE".equals(q.getType())) {
                    myAnswers.put(a.getQuestionId(), VariationValidator.judgeText(a.getAnswer()));
                } else if ("CHOICE".equals(q.getType())) {
                    myAnswers.put(a.getQuestionId(), choiceAnswerText(q, a.getAnswer()));
                }
            }
        }

        model.addAttribute("fillQuestions", fill);
        model.addAttribute("judgeQuestions", judge);
        Map<Long, List<String>> choiceOptions = new HashMap<>();
        for (VariationQuestion q : choice) {
            ChoiceAnswer choiceAnswer = VariationChoiceCodec.decode(q.getAnswer());
            List<String> labels = new ArrayList<>();
            for (List<String> values : choiceAnswer.getOptions()) {
                labels.add(VariationChoiceCodec.optionText(values));
            }
            choiceOptions.put(q.getId(), labels);
        }
        model.addAttribute("choiceQuestions", choice);
        model.addAttribute("choiceOptions", choiceOptions);
        model.addAttribute("submittedIds", submittedIds);
        model.addAttribute("myAnswers", myAnswers);
        return "variation/learn";
    }

    @PostMapping("/submit")
    @ResponseBody
    public Map<String, Object> submit(@RequestBody VariationAnswerForm form, Principal principal) {
        if (form.getQuestionId() == null) {
            return fail("题目信息丢失，刷新后再试～");
        }
        VariationQuestion question = questionMapper.selectById(form.getQuestionId());
        if (question == null) {
            return fail("题目不存在，刷新后再试～");
        }

        // 每道题只能提交一次
        SysUser me = currentUser(principal);
        if (me != null) {
            Long done = answerMapper.selectCount(new LambdaQueryWrapper<VariationAnswer>()
                    .eq(VariationAnswer::getStudentId, me.getId())
                    .eq(VariationAnswer::getQuestionId, question.getId()));
            if (done != null && done > 0) {
                return fail("你已经提交过答案啦～");
            }
        }

        boolean correct;
        String storedAnswer;

        if ("FILL".equals(question.getType())) {
            List<String> correctAnswers = parseAnswers(question.getAnswer());
            List<String> studentAnswers = new ArrayList<>();
            if (form.getAnswers() != null) {
                for (String a : form.getAnswers()) {
                    studentAnswers.add(VariationValidator.normalize(a));
                }
            }
            correct = VariationValidator.checkFill(correctAnswers, studentAnswers);
            storedAnswer = toJson(studentAnswers);
        } else if ("CHOICE".equals(question.getType())) {
            ChoiceAnswer choice = VariationChoiceCodec.decode(question.getAnswer());
            Integer idx = form.getChoiceAnswer();
            if (idx == null || idx < 0 || idx >= choice.getOptions().size()) {
                return fail("请选择一个选项～");
            }
            correct = idx == choice.getCorrect();
            storedAnswer = String.valueOf(idx);
        } else {
            String studentAnswer = "FALSE".equalsIgnoreCase(form.getJudgeAnswer()) ? "FALSE" : "TRUE";
            correct = VariationValidator.checkJudge(question.getAnswer(), studentAnswer);
            storedAnswer = studentAnswer;
        }

        VariationAnswer record = new VariationAnswer();
        record.setQuestionId(question.getId());
        record.setAnswer(storedAnswer);
        record.setCorrect(correct ? 1 : 0);
        if (me != null) {
            record.setStudentId(me.getId());
            record.setStudentName(StringUtils.hasText(me.getNickname()) ? me.getNickname() : me.getUsername());
        }
        answerMapper.insert(record);

        // 学生端不展示对错，只提示提交成功；是否正确由后台记录，供后续使用
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "提交成功！");
        return result;
    }

    private String choiceAnswerText(VariationQuestion q, String studentAnswer) {
        try {
            int idx = Integer.parseInt(studentAnswer == null ? "" : studentAnswer.trim());
            ChoiceAnswer choice = VariationChoiceCodec.decode(q.getAnswer());
            if (idx >= 0 && idx < choice.getOptions().size()) {
                return VariationChoiceCodec.letter(idx) + ". "
                        + VariationChoiceCodec.optionText(choice.getOptions().get(idx));
            }
        } catch (NumberFormatException ignored) {
            // 忽略
        }
        return studentAnswer == null ? "" : studentAnswer;
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

    private String toJson(List<String> list) {
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
