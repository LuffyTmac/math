package com.math.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;

/**
 * 自定义错误页控制器
 * 统一处理 403 / 404 / 500 等错误，渲染卡通友好提示页（含数学元素），
 * 不再显示 Spring Boot 默认的 Whitelabel 错误页。
 */
@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusObj = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int code = (statusObj != null) ? Integer.parseInt(statusObj.toString()) : 500;

        String emoji;
        String title;
        String message;

        switch (code) {
            case 403:
                emoji = "🔒";
                title = "哎呀，进不去呢！";
                message = "这个区域只对管理员小老师开放哦～ 如果你只是普通小朋友，去别的地方玩玩吧！";
                break;
            case 404:
                emoji = "🔍";
                title = "页面走丢啦！";
                message = "这道题好像被橡皮擦掉了，我们找不到它在哪里～ 检查一下网址对不对吧！";
                break;
            case 500:
                emoji = "🤖";
                title = "服务器打盹了！";
                message = "哎呀，计算器突然罢工了，小管理员正在修理，稍后再来看看吧！";
                break;
            default:
                emoji = "❓";
                title = "出了点小状况！";
                message = "好像遇到了一个没见过的数学题，先回登录页看看吧～";
        }

        model.addAttribute("code", code);
        model.addAttribute("emoji", emoji);
        model.addAttribute("title", title);
        model.addAttribute("message", message);
        return "error/error";
    }
}
