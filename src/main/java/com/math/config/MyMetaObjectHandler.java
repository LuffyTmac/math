package com.math.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 *
 * 配合实体上的 @TableField(fill = FieldFill.INSERT / INSERT_UPDATE) 注解使用。
 * - insert 时自动填充 createTime、updateTime
 * - update 时自动填充 updateTime
 *
 * 若不配置此 Bean，标注了 fill 的字段在 INSERT 时会被 MyBatis-Plus 显式置为 null，
 * 导致数据库 NOT NULL 约束报错（即使表定义了 DEFAULT CURRENT_TIMESTAMP 也不生效，
 * 因为 MySQL 收到显式 null 时不会使用默认值）。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
