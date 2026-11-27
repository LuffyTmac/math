package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.math.entity.ShelfAnswer;
import com.math.mapper.ShelfAnswerMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义书架 · 后台查看学生提交的组合（每页少量，渲染成书架+书本）。
 */
@Controller
@RequestMapping("/shelf/admin")
public class ShelfAdminController {

    /** 每页展示的提交数量（每行 2 个） */
    private static final int PAGE_SIZE = 4;

    /** 单层最多画多少本书，超出用 +N 表示 */
    private static final int MAX_BOOKS_DRAW = 24;

    @Autowired
    private ShelfAnswerMapper answerMapper;

    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "1") int page, Model model) {
        long total = answerMapper.selectCount(new LambdaQueryWrapper<>());
        int pages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.min(Math.max(1, page), pages);
        int offset = (current - 1) * PAGE_SIZE;

        List<ShelfAnswer> list = answerMapper.selectList(
                new LambdaQueryWrapper<ShelfAnswer>()
                        .orderByDesc(ShelfAnswer::getId)
                        .last("LIMIT " + offset + "," + PAGE_SIZE));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ShelfAnswer a : list) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", a.getId());
            row.put("studentName", a.getStudentName() == null ? "学生" : a.getStudentName());
            row.put("time", a.getCreateTime() == null ? "" : a.getCreateTime().toString().replace('T', ' '));
            row.put("leftLayers", a.getLeftLayers());
            row.put("leftBooks", a.getLeftBooks());
            row.put("rightLayers", a.getRightLayers());
            row.put("rightBooks", a.getRightBooks());
            row.put("leftShow", Math.min(MAX_BOOKS_DRAW, a.getLeftBooks() == null ? 0 : a.getLeftBooks()));
            row.put("leftMore", Math.max(0, (a.getLeftBooks() == null ? 0 : a.getLeftBooks()) - MAX_BOOKS_DRAW));
            row.put("rightShow", Math.min(MAX_BOOKS_DRAW, a.getRightBooks() == null ? 0 : a.getRightBooks()));
            row.put("rightMore", Math.max(0, (a.getRightBooks() == null ? 0 : a.getRightBooks()) - MAX_BOOKS_DRAW));
            rows.add(row);
        }

        model.addAttribute("rows", rows);
        model.addAttribute("page", current);
        model.addAttribute("pages", pages);
        model.addAttribute("total", total);
        model.addAttribute("hasPrev", current > 1);
        model.addAttribute("hasNext", current < pages);
        return "shelf/admin_list";
    }
}
