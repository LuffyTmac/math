package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.math.entity.EquationAnswer;
import com.math.mapper.EquationAnswerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义等式看板：展示所有学生提交的等式（ADMIN / TEACHER）。
 */
@Controller
@RequestMapping("/equation/admin")
public class EquationBoardController {

    @Autowired
    private EquationAnswerMapper answerMapper;

    @GetMapping("/board")
    public String board(Model model) {
        List<EquationAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<EquationAnswer>()
                        .orderByDesc(EquationAnswer::getId));
        long maxId = answers.isEmpty() ? 0L : answers.get(0).getId();

        model.addAttribute("answers", answers);
        model.addAttribute("total", answers.size());
        model.addAttribute("maxId", maxId);
        return "equation/admin_board";
    }

    /** 轮询新提交的等式 */
    @GetMapping("/answers")
    @ResponseBody
    public Map<String, Object> answersSince(@RequestParam(defaultValue = "0") Long sinceId) {
        long since = sinceId == null ? 0L : sinceId;
        List<EquationAnswer> fresh = answerMapper.selectList(
                new LambdaQueryWrapper<EquationAnswer>()
                        .gt(EquationAnswer::getId, since)
                        .orderByAsc(EquationAnswer::getId));

        List<Map<String, Object>> items = new ArrayList<>();
        long maxId = since;
        for (EquationAnswer a : fresh) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", a.getId());
            item.put("expression", a.getExpression());
            item.put("correct", a.getCorrect() != null && a.getCorrect() == 1);
            item.put("studentName", a.getStudentName() == null ? "学生" : a.getStudentName());
            items.add(item);
            if (a.getId() != null && a.getId() > maxId) {
                maxId = a.getId();
            }
        }

        Long total = answerMapper.selectCount(new LambdaQueryWrapper<>());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("maxId", maxId);
        result.put("total", total == null ? 0L : total);
        result.put("items", items);
        return result;
    }
}
