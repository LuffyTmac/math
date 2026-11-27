package com.math.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 面积割补 · 学生端互动页（纯前端演示，不与后台交互）
 */
@Controller
public class AreaController {

    @GetMapping("/learn/area")
    public String page() {
        return "student/area";
    }
}
