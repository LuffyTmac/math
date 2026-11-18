package com.math;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 数学教学网站 - 主启动类
 *
 * @MapperScan 扫描 com.math.mapper 包下的所有 Mapper 接口，
 * 注册为 MyBatis 代理 Bean，可直接在 Service / Controller 中注入使用。
 */
@SpringBootApplication
@MapperScan("com.math.mapper")
public class MathApplication {

    public static void main(String[] args) {
        SpringApplication.run(MathApplication.class, args);
    }
}
