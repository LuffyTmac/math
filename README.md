# 数学教学网站 (math-website)

基于 **Spring Boot + MySQL + Maven + Spring Security + Thymeleaf + MyBatis-Plus** 的数学教学网站，含用户管理与问题悬赏模块。

## 技术栈

| 组件 | 版本 / 说明 |
|------|------------|
| Spring Boot | 2.7.18 |
| Spring Security | 数据库认证 + BCrypt 加密 + 角色跳转 |
| Thymeleaf | 含 springsecurity5 集成（sec:authorize / sec:authentication） |
| MyBatis-Plus | 3.5.5（分页插件 + 逻辑删除 + LambdaQuery/Update） |
| MySQL | 驱动 8.0.33 |
| Hutool | 5.8.25 |
| Lombok | 简化实体类 |
| Java | 1.8+（本机实测 Java 21 可用） |

## 功能模块

### 用户管理（admin）
- 用户列表（搜索、分页、状态筛选）
- 添加 / 编辑 / 删除（逻辑删除）/ 重置密码
- 用户启用/禁用（禁用用户无法登录）
- username 全局唯一约束
- admin 可编辑任何账号（含 ADMIN 角色），但不能编辑自己

### 问题悬赏模块
- **admin**：发布题目（富文本编辑器，计算题/应用题/口算题）、设置悬赏星星（1-5⭐）、保存草稿/发布、查看答题情况、评分（0-5⭐）
- **user**：浏览悬赏大厅（公开）、答题（富文本输入解题思路）、查看"我的答题"与评分结果
- **登录跳转**：ADMIN → `/user/list`，USER → `/bounty/hall`

## 目录结构

```
math/
├── pom.xml
├── README.md
├── db
│   ├── schema.sql              # sys_user 建表 + admin 初始化（BCrypt 密文）
│   └── schema_bounty.sql       # 悬赏模块建表（bounty_question + bounty_answer）
└── src/main
    ├── java/com/math
    │   ├── MathApplication.java              # 主启动类（@MapperScan）
    │   ├── config/
    │   │   ├── SecurityConfig.java           # 安全配置（DB认证 + 角色权限 + 跳转）
    │   │   ├── MybatisPlusConfig.java        # 分页插件 + 禁用 DdlApplicationRunner
    │   │   └── CustomAuthenticationFailureHandler.java  # 登录失败处理（禁用提示）
    │   ├── handler/
    │   │   └── CustomAuthenticationSuccessHandler.java  # 登录成功按角色跳转
    │   ├── entity/
    │   │   ├── SysUser.java                  # 用户实体（role/status/deleted）
    │   │   ├── BountyQuestion.java           # 悬赏题目实体
    │   │   └── BountyAnswer.java             # 答题实体
    │   ├── mapper/
    │   │   ├── SysUserMapper.java
    │   │   ├── BountyQuestionMapper.java
    │   │   └── BountyAnswerMapper.java
    │   ├── service/
    │   │   └── CustomUserDetailsService.java # 数据库认证（status 禁用校验）
    │   ├── util/
    │   │   └── XssUtil.java                  # 富文本 XSS 清理
    │   └── controller/
    │       ├── AdminController.java          # /index 首页
    │       ├── UserController.java           # /user/** 用户管理
    │       ├── BountyController.java         # /bounty/** 前台（大厅/答题/我的答题）
    │       ├── AdminBountyController.java    # /bounty/admin/** 题目管理/评分
    │       ├── CustomErrorController.java    # 卡通错误页（403/404/500）
    │       └── HelloController.java          # /hello 测试页
    └── resources
        ├── application.yml
        ├── static/
        │   ├── css/admin.css                 # 后台 + 悬赏模块样式
        │   └── js/rich-editor.js             # 轻量富文本编辑器
        └── templates/
            ├── layout/common.html            # 侧边栏 + 顶部栏片段（按角色显示）
            ├── login.html / index.html / hello.html / error/
            ├── user_*.html                   # 用户管理页面
            └── bounty/
                ├── hall.html                 # 悬赏大厅（公开）
                ├── answer.html               # 答题页
                ├── my_answers.html           # 我的答题列表
                ├── my_answer_detail.html     # 我的答题详情
                ├── admin_list.html           # 题目管理列表
                ├── admin_edit.html           # 添加/编辑题目
                └── admin_answers.html        # 答题情况 + 评分
```

## 数据库

### sys_user（用户表）
| 字段 | 说明 |
|------|------|
| id, username, password, nickname | 基本信息 |
| role | ADMIN / USER |
| status | 1启用 0禁用（禁用用户无法登录） |
| deleted | 逻辑删除 |
| create_time, update_time | 时间戳 |

### bounty_question（悬赏题目表）
| 字段 | 说明 |
|------|------|
| id, title, content | 题目标题 + 富文本内容 |
| question_type | CALC计算题 / APPLICATION应用题 / ORAL口算题 |
| star_count | 悬赏星星数（1-5） |
| status | DRAFT草稿 / PUBLISHED已发布 |
| creator_id, publish_time | 创建人 / 发布时间 |
| deleted | 逻辑删除 |

### bounty_answer（答题表）
| 字段 | 说明 |
|------|------|
| id, question_id, answer_user_id | 关联信息 |
| answer_content | 答题富文本内容 |
| submit_time | 提交时间 |
| score | 评分星星数（0-5，未评分时 null） |
| score_status | UNSCORED未评分 / SCORED已评分 |
| scorer_id, score_time | 评分人 / 评分时间 |

## 访问地址

| 路径 | 权限 | 说明 |
|------|------|------|
| `/bounty/hall` | 公开 | 悬赏大厅（卡片式，按时间/星星排序，分页） |
| `/login` | 公开 | 登录页（卡通风格） |
| `/hello` | 公开 | 测试页 |
| `/bounty/answer` | 登录 | 答题（GET表单 / POST提交，同一题只能答一次） |
| `/bounty/my-answers` | 登录 | 我的答题列表 |
| `/bounty/my-answer` | 登录 | 我的答题详情（题目+答案+评分） |
| `/index` | ADMIN | 后台首页 |
| `/user/list` | ADMIN | 用户管理（搜索/分页/增删改/禁用） |
| `/bounty/admin/list` | ADMIN | 题目管理列表（含答题统计） |
| `/bounty/admin/add` | ADMIN | 发布题目（富文本 + 星星选择器） |
| `/bounty/admin/edit` | ADMIN | 编辑题目 |
| `/bounty/admin/answers` | ADMIN | 查看答题 + 评分 |
| `/logout` | 公开 | 退出（POST） |

## 运行前准备

### 1. 初始化数据库

```bash
mysql -uroot -p math < db/schema.sql          # 用户表 + admin 初始化
mysql -uroot -p math < db/schema_bounty.sql   # 悬赏模块表
```

确认 `application.yml` 中 `spring.datasource` 配置正确。

### 2. 启动

```bash
mvn spring-boot:run
```

### 3. 访问

- http://localhost:8080/bounty/hall — 悬赏大厅（免登录）
- http://localhost:8080/login — 登录（admin/hxx123456 或自建用户）
- admin 登录后跳转 `/user/list`，user 登录后跳转 `/bounty/hall`

## 关键实现说明

- **登录跳转**：`CustomAuthenticationSuccessHandler` 按角色跳转（ADMIN→/user/list，USER→/bounty/hall）。
- **禁用校验**：`CustomUserDetailsService` 用 `User.builder().disabled(status==0)`，`CustomAuthenticationFailureHandler` 捕获 `DisabledException` 跳转 `/login?disabled`。
- **富文本编辑器**：`static/js/rich-editor.js` 基于 `contenteditable + execCommand`，无外部依赖，存储为 HTML。
- **XSS 防护**：`XssUtil.clean()` 清理 script 标签、on* 事件属性、javascript: 协议。
- **题目预览**：控制器用正则去 HTML 标签生成纯文本预览（截断 100 字）。
- **分页**：MyBatis-Plus `PaginationInnerInterceptor`，列表页支持每页条数切换。
- **侧边栏**：`common.html` 用 `sec:authorize` 按角色显示菜单（USER 看大厅/我的答题，ADMIN 额外看管理后台）。
