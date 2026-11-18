package com.math.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.math.entity.SysUser;
import com.math.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 基于数据库的用户认证服务
 *
 * 替代原先的内存用户。通过 SysUserMapper 查询 sys_user 表，
 * 将数据库中的用户名、BCrypt 密文密码、角色转换为 Spring Security 的 UserDetails。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在：" + username);
        }
        // role 字段存的是 ADMIN / TEACHER / STUDENT，.roles() 会自动加 ROLE_ 前缀
        //   ADMIN   → ROLE_ADMIN   （全部功能）
        //   TEACHER → ROLE_TEACHER （悬赏管理，无用户管理权限）
        //   STUDENT → ROLE_STUDENT （仅学习天地）
        // status：1 启用，0 禁用；禁用账号设置 disabled=true，Spring Security 认证时会抛 DisabledException
        boolean enabled = user.getStatus() != null && user.getStatus() == 1;
        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .disabled(!enabled)
                .build();
    }
}
