package com.math.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/**
 * 后台控制器
 * 访问其中的页面需要先登录（由 SecurityConfig 控制）。
 */
@Controller
public class AdminController {

    /**
     * 首页（需登录）
     * 登录成功后会跳转到此页面。当前仅展示基础信息，后期再扩展教学功能模块。
     */
    @GetMapping("/index")
    public String index(Model model, Principal principal) {
        String username = (principal != null) ? principal.getName() : "访客";
        model.addAttribute("username", username);
        model.addAttribute("title", "数学教学后台");
        model.addAttribute("content",
                "欢迎使用数学教学网站后台管理系统。当前为基础版本，" +
                "后续将逐步添加题目管理、课程管理、用户管理、统计分析等功能模块。");
        return "index";
    }
}
