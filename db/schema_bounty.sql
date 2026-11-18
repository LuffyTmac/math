-- ============================================================
-- 悬赏模块表结构：bounty_question（题目） + bounty_answer（答题）
-- 执行：mysql --default-character-set=utf8mb4 -u<user> -p<pwd> math < db/schema_bounty.sql
-- ============================================================

-- 悬赏题目表
DROP TABLE IF EXISTS `bounty_question`;
CREATE TABLE `bounty_question` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT        COMMENT '主键',
    `title`         VARCHAR(120) NOT NULL                       COMMENT '题目标题（列表展示用）',
    `content`       MEDIUMTEXT   NOT NULL                       COMMENT '题目内容（富文本 HTML）',
    `question_type` VARCHAR(20)  NOT NULL DEFAULT 'CALC'        COMMENT '题目类型：CALC 计算题 / APPLICATION 应用题 / ORAL 口算题',
    `star_count`    INT          NOT NULL DEFAULT 1             COMMENT '悬赏星星数（admin 设置）',
    `status`        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'       COMMENT '状态：DRAFT 草稿 / PUBLISHED 已发布',
    `hint_content`  MEDIUMTEXT   NULL                           COMMENT '提示与思路（富文本 HTML，admin 可选填写）',
    `hint_visible`  TINYINT      NOT NULL DEFAULT 0             COMMENT '提示是否展示：0 隐藏 1 展示（admin 手动开启）',
    `creator_id`    BIGINT       NULL                           COMMENT '创建人 id（admin）',
    `publish_time`  DATETIME     NULL                           COMMENT '发布时间（用于排序）',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                        COMMENT '创建时间',
    `update_time`   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除：0 未删 1 已删',
    PRIMARY KEY (`id`),
    KEY `idx_status`      (`status`),
    KEY `idx_creator`     (`creator_id`),
    KEY `idx_publish_time`(`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='悬赏题目';

-- 答题表
DROP TABLE IF EXISTS `bounty_answer`;
CREATE TABLE `bounty_answer` (
    `id`             BIGINT      NOT NULL AUTO_INCREMENT        COMMENT '主键',
    `question_id`    BIGINT      NOT NULL                       COMMENT '题目 id',
    `answer_user_id` BIGINT      NOT NULL                       COMMENT '答题人 id',
    `answer_content` MEDIUMTEXT  NOT NULL                       COMMENT '答题内容（富文本 HTML：解题思路+答案）',
    `submit_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                        COMMENT '提交时间',
    `score`          INT         NULL                           COMMENT '评分星星数（admin 评分）',
    `score_status`   VARCHAR(20) NOT NULL DEFAULT 'UNSCORED'    COMMENT '评分状态：UNSCORED 未评分 / SCORED 已评分',
    `scorer_id`      BIGINT      NULL                           COMMENT '评分人 id（admin）',
    `score_time`     DATETIME    NULL                           COMMENT '评分时间',
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP                        COMMENT '创建时间',
    `update_time`    DATETIME    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT     NOT NULL DEFAULT 0             COMMENT '逻辑删除：0 未删 1 已删',
    PRIMARY KEY (`id`),
    KEY `idx_question`      (`question_id`),
    KEY `idx_answer_user`   (`answer_user_id`),
    KEY `idx_score_status`  (`score_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='悬赏答题';
