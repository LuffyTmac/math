package com.math.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体（示例）
 *
 * 对应数据库表 sys_user，见 db/schema.sql。
 * 演示 MyBatis-Plus 的常用注解：
 *   - @TableName  表名映射
 *   - @TableId    主键策略（数据库自增）
 *   - @TableLogic 逻辑删除字段（配合 application.yml 的全局配置）
 *
 * 字段采用驼峰命名，MyBatis-Plus 自动映射数据库下划线字段
 * （create_time <-> createTime）。
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 角色：ADMIN / USER */
    private String role;

    /** 状态：1 启用，0 禁用 */
    private Integer status;

    /** 逻辑删除：0 未删，1 已删 */
    @TableLogic
    private Integer deleted;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
