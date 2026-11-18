package com.math;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 临时工具测试：生成 BCrypt 密码哈希，用于 sys_user 初始化数据。
 * 用完可删除。运行：mvn -Dtest=PasswordHashTest test
 */
class PasswordHashTest {

    @Test
    void genHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("hxx123456");
        // 用标记包裹，便于从输出中提取
        System.out.println("HASH_START=" + hash + "=HASH_END");
        System.out.println("verify matches=" + encoder.matches("hxx123456", hash));
    }
}
