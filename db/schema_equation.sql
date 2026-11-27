-- ============================================================
-- 自定义等式模块建表脚本
-- ============================================================
CREATE TABLE IF NOT EXISTS `equation_answer` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT        COMMENT '主键',
    `expression`    VARCHAR(255) NOT NULL                       COMMENT '学生提交的等式（乘号用 ×，已去空格）',
    `is_correct`    TINYINT      NOT NULL DEFAULT 0             COMMENT '是否正确：1 正确 0 错误（错误也保留，便于展示）',
    `student_id`    BIGINT       NULL                           COMMENT '提交学生 id（sys_user.id）',
    `student_name`  VARCHAR(64)  NULL                           COMMENT '提交学生姓名（昵称优先）',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '提交时间',
    `update_time`   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除：0 未删 1 已删',
    PRIMARY KEY (`id`),
    KEY `idx_student_time` (`student_id`, `create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义等式 · 学生提交';
