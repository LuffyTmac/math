-- ============================================================
-- 乘法分配律模块表结构：distributive_question（题目） + distributive_answer（学生算式）
-- 执行：mysql --default-character-set=utf8mb4 -u<user> -p<pwd> math < db/schema_distributive.sql
--
-- 注意：脚本包含 DROP TABLE IF EXISTS，重复执行会清空本模块数据（与其它 schema 文件一致）。
-- ============================================================

-- ------------------------------------------------------------
-- 题目表
--   title 只用于编辑/预览/图片说明，列表卡片与学习页卡片只展示 image_url
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `distributive_question`;
CREATE TABLE `distributive_question` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT        COMMENT '主键',
    `title`        VARCHAR(200) NOT NULL                       COMMENT '题目名称（编辑页/预览用，卡片不展示）',
    `image_url`    VARCHAR(500) NULL                           COMMENT '图片链接（卡片与甬道上唯一展示的内容）',
    `description`  MEDIUMTEXT   NULL                           COMMENT '详细说明（富文本 HTML，学生答题层「小提示」折叠展示）',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'       COMMENT '状态：DRAFT 草稿 / PUBLISHED 已发布',
    `sort_order`   INT          NOT NULL DEFAULT 0             COMMENT '排序号（越小越靠前，决定第几题/第几条甬道）',
    `creator_id`   BIGINT       NULL                           COMMENT '创建人 id（ADMIN / TEACHER）',
    `publish_time` DATETIME     NULL                           COMMENT '首次发布时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                        COMMENT '创建时间',
    `update_time`  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除：0 未删 1 已删',
    PRIMARY KEY (`id`),
    KEY `idx_status_sort` (`status`, `sort_order`),
    KEY `idx_deleted`     (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='乘法分配律题目';

-- ------------------------------------------------------------
-- 学生算式表（不记录提交人身份，提交即展示到教师端云海）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `distributive_answer`;
CREATE TABLE `distributive_answer` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT        COMMENT '主键',
    `question_id` BIGINT       NOT NULL                       COMMENT '所属题目 id',
    `expression`  VARCHAR(200) NOT NULL                       COMMENT '规范化后的算式（乘号用 ×），如 3×(4+5) 或 3×4+3×5',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                        COMMENT '提交时间（甬道内排序）',
    `update_time` DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0             COMMENT '逻辑删除：0 未删 1 已删（教师可删乱填内容）',
    PRIMARY KEY (`id`),
    KEY `idx_question_time` (`question_id`, `create_time`),
    KEY `idx_deleted`       (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='乘法分配律学生算式';

-- ------------------------------------------------------------
-- 可选：4 道示例题目（首次部署想看效果时取消注释；图片链接换成你自己的）
-- ------------------------------------------------------------
-- INSERT INTO `distributive_question` (`title`, `image_url`, `status`, `sort_order`, `publish_time`)
-- VALUES
--   ('一共有多少块巧克力？', '/uploads/2026/11/demo1.png', 'PUBLISHED', 1, NOW()),
--   ('每盒有多少支铅笔？',   '/uploads/2026/11/demo2.png', 'PUBLISHED', 2, NOW()),
--   ('小明的果园',           '/uploads/2026/11/demo3.png', 'PUBLISHED', 3, NOW()),
--   ('班级图书角',           '/uploads/2026/11/demo4.png', 'PUBLISHED', 4, NOW());
