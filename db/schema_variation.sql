-- ============================================================
-- 变式练习模块建表脚本
-- ============================================================
CREATE TABLE IF NOT EXISTS `variation_question` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT        COMMENT '主键',
    `type`        VARCHAR(10)  NOT NULL                       COMMENT '类型：FILL 填空 / JUDGE 判断',
    `content`     VARCHAR(500) NOT NULL                       COMMENT '题干：填空用 □ 表示空格；判断为完整算式',
    `answer`      TEXT         NULL                           COMMENT '填空：JSON 数组（每空一个答案）；判断：TRUE/FALSE',
    `sort_order`  INT          NOT NULL DEFAULT 0             COMMENT '排序，数字越小越靠前',
    `creator_id`  BIGINT       NULL                           COMMENT '出题人 id',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '创建时间',
    `update_time` DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_type_sort` (`type`, `sort_order`),
    KEY `idx_deleted` (`deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '变式练习 · 题目';

CREATE TABLE IF NOT EXISTS `variation_answer` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT        COMMENT '主键',
    `question_id`  BIGINT      NOT NULL                       COMMENT '题目 id',
    `student_id`   BIGINT      NULL                           COMMENT '学生 id',
    `student_name` VARCHAR(64) NULL                           COMMENT '学生姓名',
    `answer`       TEXT        NULL                           COMMENT '学生答案：填空 JSON 数组 / 判断 TRUE|FALSE',
    `is_correct`   TINYINT     NOT NULL DEFAULT 0             COMMENT '是否正确：1 正确 0 错误',
    `create_time`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                       COMMENT '提交时间',
    `update_time`  DATETIME    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT     NOT NULL DEFAULT 0             COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_question` (`question_id`),
    KEY `idx_student` (`student_id`, `create_time`),
    KEY `idx_deleted` (`deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '变式练习 · 学生作答';
