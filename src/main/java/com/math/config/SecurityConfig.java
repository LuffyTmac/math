package com.math.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.math.handler.CustomAuthenticationSuccessHandler;

/**
 * Spring Security 配置
 *
 * 当前阶段：使用内存方式验证用户。
 *   - 用户名：admin
 *   - 密码：  hxx123456
 *   - 角色：  ADMIN
 *
 * 访问规则：
 *   - /hello、/login、静态资源：无需登录即可访问
 *   - 其它请求（如 /index）：必须登录
 *
 * 后期可将 userDetailsService 替换为基于数据库的实现。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** BCrypt 密码编码器 */
    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;

    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 用户认证已迁移到数据库，见 com.math.service.CustomUserDetailsService（@Service 自动注册为 UserDetailsService）。
     * 不再使用内存用户。如需新增用户，请通过 /user/add 页面操作（密码 BCrypt 加密入库）。
     */

    /**
     * 安全过滤链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 授权规则
            .authorizeRequests()
                // 公开访问
                .antMatchers("/hello", "/login", "/error", "/bounty/hall", "/css/**", "/js/**", "/images/**", "/uploads/**", "/webjars/**").permitAll()
                // 后台首页与用户管理：仅 ADMIN 角色（管理员唯一，默认只有一个）
                .antMatchers("/index", "/user/**").hasRole("ADMIN")
                // 悬赏管理（出题/评分/题目管理）：ADMIN + TEACHER
                .antMatchers("/bounty/admin/**").hasAnyRole("ADMIN", "TEACHER")
                // 图片上传接口：任何已登录用户（admin/teacher/student）都可使用
                .antMatchers("/upload/**").authenticated()
                // 其它请求（如 /bounty/hall /bounty/answer /bounty/my-answers 等）均需登录
                .anyRequest().authenticated()
            .and()
            // 表单登录
            .formLogin()
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .successHandler(authenticationSuccessHandler)
                .failureHandler(authenticationFailureHandler)
                .permitAll()
            .and()
            // 退出登录
            .logout()
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            .and()
            // CSRF：/hello 是纯展示页，若后续做接口再按需放行
            .csrf().ignoringAntMatchers("/hello");

        return http.build();
    }
}
