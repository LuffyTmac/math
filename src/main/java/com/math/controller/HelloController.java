package com.math.controller;

import cn.hutool.core.date.DateUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 公开测试控制器
 * 该控制器下的页面无需登录即可访问（已在 SecurityConfig 中放行）。
 */
@Controller
public class HelloController {

    /**
     * 测试入口：/hello
     * 无需认证，用于验证 Spring Security 放行规则与 Thymeleaf 渲染链路。
     * 此处顺便使用 Hutool 工具包获取当前时间，验证依赖可用。
     */
    @GetMapping("/hello")
    public String hello(Model model) {
        model.addAttribute("now", DateUtil.now());
        return "hello";
    }

    /**
     * 登录页面入口：/login (GET)
     * 注意：POST /login 由 Spring Security 拦截处理认证，
     * 这里仅返回自定义登录页面（公开访问）。
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
