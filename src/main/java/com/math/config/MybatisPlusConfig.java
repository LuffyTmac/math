package com.math.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.DdlApplicationRunner;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * 目前注册了分页插件（PaginationInnerInterceptor），
 * 使用 Mapper 时调用 IPage 相关方法即可自动分页。
 * 后续可按需加乐观锁、防全表更新等 InnerInterceptor。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件：指定数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 禁用 MyBatis-Plus 的 DdlApplicationRunner（启动时自动 DDL 维护）。
     * <p>
     * MybatisPlusAutoConfiguration 通过 @ConditionalOnMissingBean 注册 DdlApplicationRunner，
     * 这里提供一个 run 为空操作的 Bean 覆盖它，使其不再执行任何 DDL。
     * 如后续需要自动建表/DDL 维护，删除此 Bean 即可恢复默认行为。
     */
    @Bean
    public DdlApplicationRunner ddlApplicationRunner() {
        return new DdlApplicationRunner(java.util.Collections.emptyList()) {
            @Override
            public void run(ApplicationArguments args) {
                // no-op：禁用 MyBatis-Plus 启动时的 DDL 自动维护
            }
        };
    }
}
