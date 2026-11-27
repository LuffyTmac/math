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
 * - ADMIN（管理员）   → /index（后台首页）
 * - TEACHER（教师）   → /index（后台首页）
 * - STUDENT（学生）   → /journey（我的旅程）
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    /** 管理员 / 教师登录后进入后台首页 */
    private static final String ADMIN_HOME = "/index";

    /** 学生登录后进入「我的旅程」 */
    private static final String STUDENT_HOME = "/journey";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String targetUrl = determineTargetUrl(authentication);
        response.sendRedirect(targetUrl);
    }

    private String determineTargetUrl(Authentication authentication) {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        // roles() 会自动加 ROLE_ 前缀 → ROLE_ADMIN / ROLE_TEACHER / ROLE_STUDENT
        for (GrantedAuthority auth : authorities) {
            String role = auth.getAuthority();
            if ("ROLE_ADMIN".equals(role) || "ROLE_TEACHER".equals(role)) {
                return ADMIN_HOME;
            }
        }
        // 学生（以及其它未匹配角色）→ 我的旅程
        return STUDENT_HOME;
    }
}
