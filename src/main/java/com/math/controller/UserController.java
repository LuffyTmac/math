package com.math.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.math.entity.SysUser;
import com.math.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/**
 * 用户管理控制器
 * 路径 /user/** 在 SecurityConfig 中限制为 ADMIN 角色才能访问。
 *
 * 功能：关键字搜索 + 分页 + 添加 + 删除（逻辑删除）+ 重置密码。
 */
@Controller
@RequestMapping("/user")
public class UserController {

    /** 允许的每页条数 */
    private static final long[] ALLOWED_SIZES = {5, 10, 20, 50};
    private static final long DEFAULT_SIZE = 10;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户列表（支持关键字搜索 + 分页）
     *
     * @param page    当前页码，从 1 开始
     * @param size    每页条数（仅允许 5 / 10 / 20 / 50）
     * @param keyword 关键字（模糊匹配用户名 / 昵称）
     */
    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "1") long page,
                       @RequestParam(defaultValue = "10") long size,
                       @RequestParam(required = false) String keyword,
                       Model model) {
        // 校验每页条数
        if (!inAllowedSizes(size)) {
            size = DEFAULT_SIZE;
        }

        Page<SysUser> p = new Page<>(Math.max(page, 1), size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                              .or().like(SysUser::getNickname, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> result = sysUserMapper.selectPage(p, wrapper);

        // 转成 int 便于模板中页码比较（避免 Long / Integer equals 不匹配）
        model.addAttribute("users", result.getRecords());
        model.addAttribute("total", result.getTotal());
        model.addAttribute("current", (int) Math.max(result.getCurrent(), 1));
        model.addAttribute("pages", (int) result.getPages());
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("size", (int) size);
        return "user_list";
    }

    /** 添加用户 - 展示表单 */
    @GetMapping("/add")
    public String addForm(Model model) {
        SysUser user = new SysUser();
        user.setStatus(1);  // 默认启用
        model.addAttribute("user", user);
        return "user_add";
    }

    /** 添加用户 - 提交保存（密码 BCrypt 加密后入库；用户名唯一性校验） */
    @PostMapping("/add")
    public String addSave(@ModelAttribute SysUser user, RedirectAttributes ra) {
        // 用户名唯一性兜底
        LambdaQueryWrapper<SysUser> exist = new LambdaQueryWrapper<>();
        exist.eq(SysUser::getUsername, user.getUsername());
        Long count = sysUserMapper.selectCount(exist);
        if (count != null && count > 0) {
            ra.addFlashAttribute("msg", "❌ 用户名「" + user.getUsername() + "」已存在，请换一个！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/add";
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 角色合法性校验：添加用户只能选择 TEACHER / STUDENT（不允许创建 ADMIN，管理员唯一）
        if (!"TEACHER".equals(user.getRole()) && !"STUDENT".equals(user.getRole())) {
            user.setRole("STUDENT");
        }
        // status 默认启用（1）
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        try {
            sysUserMapper.insert(user);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 数据库唯一索引兜底：例如历史已删除账号仍占用该用户名（逻辑删除记录 deleted=1 仍占唯一索引）
            ra.addFlashAttribute("msg", "❌ 用户名「" + user.getUsername() + "」已被占用（可能是历史账号），请换一个！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/add";
        }
        ra.addFlashAttribute("msg", "✅ 用户「" + user.getUsername() + "」添加成功！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/user/list";
    }

    /**
     * 删除用户（逻辑删除，@TableLogic 自动转为 update deleted=1）
     * 保护：不能删除超级管理员 admin；不能删除当前登录的自己。
     */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        SysUser target = sysUserMapper.selectById(id);
        if (target == null) {
            ra.addFlashAttribute("msg", "❌ 用户不存在或已被删除！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/list";
        }
        if ("admin".equals(target.getUsername())) {
            ra.addFlashAttribute("msg", "❌ 不能删除超级管理员账号 admin！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/list";
        }
        if (principal != null && principal.getName().equals(target.getUsername())) {
            ra.addFlashAttribute("msg", "❌ 不能删除当前登录的自己！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/list";
        }
        sysUserMapper.deleteById(id);
        ra.addFlashAttribute("msg", "✅ 用户「" + target.getUsername() + "」已删除！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/user/list";
    }

    /** 重置密码 - 展示表单 */
    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam Long id, Model model, RedirectAttributes ra) {
        SysUser target = sysUserMapper.selectById(id);
        if (target == null) {
            ra.addFlashAttribute("msg", "❌ 用户不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/list";
        }
        model.addAttribute("targetUser", target);
        return "user_reset_password";
    }

    /** 重置密码 - 提交（BCrypt 加密，LambdaUpdateWrapper 只更新 password 字段） */
    @PostMapping("/reset-password")
    public String resetPasswordSave(@RequestParam Long id,
                                    @RequestParam String newPassword,
                                    @RequestParam String confirmPassword,
                                    RedirectAttributes ra) {
        SysUser target = sysUserMapper.selectById(id);
        if (target == null) {
            ra.addFlashAttribute("msg", "❌ 用户不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/list";
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            ra.addFlashAttribute("msg", "❌ 新密码至少 6 位！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/reset-password?id=" + id;
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("msg", "❌ 两次输入的密码不一致！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/reset-password?id=" + id;
        }
        LambdaUpdateWrapper<SysUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysUser::getId, id)
          .set(SysUser::getPassword, passwordEncoder.encode(newPassword));
        sysUserMapper.update(null, uw);
        ra.addFlashAttribute("msg", "✅ 用户「" + target.getUsername() + "」密码已重置！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/user/list";
    }

    /**
     * 编辑用户 - 展示表单（可编辑任何非自己的用户：昵称、角色、状态，含 ADMIN 角色）
     * 用户名、密码不在此修改：用户名为登录凭证不可变，密码请走"重置密码"。
     * 保护：不能编辑当前登录的自己（避免把自己锁死）。
     */
    @GetMapping("/edit")
    public String editForm(@RequestParam Long id, Model model, RedirectAttributes ra, Principal principal) {
        SysUser target = sysUserMapper.selectById(id);
        if (target == null) {
            ra.addFlashAttribute("msg", "❌ 用户不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/list";
        }
        if (principal != null && principal.getName().equals(target.getUsername())) {
            ra.addFlashAttribute("msg", "❌ 不能编辑当前登录的自己！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/list";
        }
        model.addAttribute("targetUser", target);
        return "user_edit";
    }

    /** 编辑用户 - 提交（LambdaUpdateWrapper 只更新昵称/角色/状态，不影响其它字段） */
    @PostMapping("/edit")
    public String editSave(@RequestParam Long id,
                           @RequestParam String nickname,
                           @RequestParam String role,
                           @RequestParam(defaultValue = "1") Integer status,
                           Principal principal,
                           RedirectAttributes ra) {
        SysUser target = sysUserMapper.selectById(id);
        if (target == null) {
            ra.addFlashAttribute("msg", "❌ 用户不存在！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/list";
        }
        if (principal != null && principal.getName().equals(target.getUsername())) {
            ra.addFlashAttribute("msg", "❌ 不能编辑当前登录的自己！");
            ra.addFlashAttribute("msgType", "error");
            return "redirect:/user/list";
        }
        // 角色合法性校验：编辑时只能设为 TEACHER / STUDENT（管理员角色不可被赋予）
        // 例外：被编辑对象是 admin 超管时，强制保持 ADMIN（防止管理员角色被降级）
        if ("admin".equals(target.getUsername())) {
            role = "ADMIN";
        } else if (!"TEACHER".equals(role) && !"STUDENT".equals(role)) {
            role = "STUDENT";
        }
        LambdaUpdateWrapper<SysUser> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysUser::getId, id)
          .set(SysUser::getNickname, nickname)
          .set(SysUser::getRole, role)
          .set(SysUser::getStatus, status == null ? 1 : status);
        sysUserMapper.update(null, uw);
        ra.addFlashAttribute("msg", "✅ 用户「" + target.getUsername() + "」信息已更新！");
        ra.addFlashAttribute("msgType", "success");
        return "redirect:/user/list";
    }

    private boolean inAllowedSizes(long size) {
        for (long s : ALLOWED_SIZES) {
            if (s == size) return true;
        }
        return false;
    }
}
