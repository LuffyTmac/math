package com.math.controller;

import com.math.entity.SysUser;
import com.math.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 用户管理控制器
 * 路径 /user/** 在 SecurityConfig 中限制为 ADMIN 角色才能访问。
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /** 用户列表 */
    @GetMapping("/list")
    public String list(Model model) {
        List<SysUser> users = sysUserMapper.selectList(null);
        model.addAttribute("users", users);
        return "user_list";
    }

    /** 添加用户 - 展示表单 */
    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("user", new SysUser());
        return "user_add";
    }

    /** 添加用户 - 提交保存（密码 BCrypt 加密后入库） */
    @PostMapping("/add")
    public String addSave(@ModelAttribute SysUser user) {
        // 密码加密存储
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 角色兜底
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("USER");
        }
        sysUserMapper.insert(user);
        return "redirect:/user/list";
    }
}
