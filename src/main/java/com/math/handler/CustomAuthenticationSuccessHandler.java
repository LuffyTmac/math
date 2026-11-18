package com.math.handler;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;

/**
 * 登录成功跳转处理器（三种角色）
 * - ADMIN（管理员）   → /index（后台首页，全部功能）
 * - TEACHER（教师）   → /bounty/admin/list（悬赏管理，可出题/评分，无用户管理权限）
 * - STUDENT（学生）   → /bounty/hall（悬赏大厅，仅学习天地功能）
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(authentication);
        response.sendRedirect(targetUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        // roles() 会自动加 ROLE_ 前缀 → ROLE_ADMIN / ROLE_TEACHER / ROLE_STUDENT
        boolean isTeacher = false;
        for (GrantedAuthority auth : authorities) {
            String role = auth.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                return "/index";
            }
            if ("ROLE_TEACHER".equals(role)) {
                isTeacher = true;
            }
        }
        // 教师登录 → 悬赏管理列表（可出题/评分/管理题目）
        if (isTeacher) {
            return "/bounty/admin/list";
        }
        // 学生登录 → 悬赏大厅
        return "/bounty/hall";
    }
}
