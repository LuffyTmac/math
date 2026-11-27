-- ============================================================
-- 自定义书架模块建表脚本
-- ============================================================
CREATE TABLE IF NOT EXISTS `shelf_answer` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT  COMMENT '主键',
    `left_layers`   INT         NOT NULL                 COMMENT '左书架层数',
    `left_books`    INT         NOT NULL                 COMMENT '左书架每层书本数',
    `right_layers`  INT         NOT NULL                 COMMENT '右书架层数',
    `right_books`   INT         NOT NULL                 COMMENT '右书架每层书本数',
    `student_id`    BIGINT      NULL                     COMMENT '提交学生 id（sys_user.id）',
    `student_name`  VARCHAR(64) NULL                     COMMENT '提交学生姓名（昵称优先）',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '提交时间',
    `update_time`   DATETIME    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT     NOT NULL DEFAULT 0       COMMENT '逻辑删除：0 未删 1 已删',
    PRIMARY KEY (`id`),
    KEY `idx_student_time` (`student_id`, `create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自定义书架 · 学生提交';
