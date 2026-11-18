-- ============================================================
-- 数学教学网站 - 用户表初始化
-- 数据库：math（本地已创建）
-- 执行方式：mysql -uroot -p math < db/schema.sql
--   或在 MySQL 客户端中粘贴执行
-- ============================================================

USE `math`;

-- ------------------------------------------------------------
-- 系统用户表（角色：ADMIN 管理员 / TEACHER 教师 / STUDENT 学生）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL                         COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL                         COMMENT '密码（BCrypt 加密后的密文）',
    `nickname`    VARCHAR(50)  DEFAULT NULL                     COMMENT '昵称',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT'       COMMENT '角色：ADMIN / TEACHER / STUDENT',
    `status`      TINYINT      NOT NULL DEFAULT 1               COMMENT '状态：1启用 0禁用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0               COMMENT '逻辑删除：0未删 1已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP        COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '系统用户表';

-- ------------------------------------------------------------
-- 初始化管理员账号（系统唯一管理员，不可创建/降级）
--   用户名：admin
--   密码：  hxx123456 （此处存的是 BCrypt 加密后的密文）
--   角色：  ADMIN
-- ------------------------------------------------------------
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `role`)
VALUES (
    'admin',
    '$2a$10$b1lvcgVcxGCJ9S7HQbfZjOjnsK65eO03nMolkjZXeDlb/qcAFnrj2',
    '管理员',
    'ADMIN'
);
