package com.math.config;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 自定义登录失败处理器
 *
 * 根据异常类型跳转到登录页并显示不同的提示：
 *   - DisabledException：账号已被禁用 → /login?disabled
 *   - 其它（密码错误 / 用户不存在等）：→ /login?error
 *
 * 配合 login.html 的 param.disabled / param.error 显示对应的卡通提示。
 */
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        if (exception instanceof DisabledException) {
            response.sendRedirect(request.getContextPath() + "/login?disabled");
        } else {
            response.sendRedirect(request.getContextPath() + "/login?error");
        }
    }
}
