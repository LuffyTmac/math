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
- **登录跳转**：ADMIN / TEACHER → `/distributive/admin/list`，STUDENT → `/distributive/learn`

### 乘法分配律模块
- **admin / teacher**：题目只维护图片（可选详细说明），保存草稿 / 发布 / 下架 / 删除（统一在「题目管理」页操作）；分配律看板为沉浸式全屏（隐藏顶部栏、深色铺满）；
  列表页是「一行四卡 + 科幻云海 + 甬道」（按 `sort_order` 取前 4 条）——默认甬道收起、卡片贴底，算式以霓虹气泡占满上方；
  点「开始归类」后卡片顶到顶部、甬道铺满下方（甬道内按 A×(B+C) / A×B＋A×C 分两栏），算式一个一个飞入对应栏；工具栏还有「🧹 擦除所有算式」可一键清空学生提交；固定 15s 自动刷新 + 手动「刷新」按钮（整页重载回到未归类状态），可删除单条算式。
- **学生 / 游客**：Pad 版学习页（按 `sort_order` 默认展示前 4 道题，满屏 2×2 大图卡片 + 漂浮数学符号，无任何标题/导航/题号文字），点卡片弹出答题层，
  用符号键盘输入算式（答题层也只保留图片和键盘）；只接受两种分配律写法：`a*(b+c)` 与 `a*b+a*c`（含减号）。成功/错误提示都弹在屏幕正中。
- 学生提交不记录身份，提交即出现在教师端云海。

## 目录结构

```
math/
├── pom.xml
├── README.md
├── db
│   ├── schema.sql              # sys_user 建表 + admin 初始化（BCrypt 密文）
│   ├── schema_bounty.sql       # 悬赏模块建表（bounty_question + bounty_answer）
│   └── schema_distributive.sql # 分配律模块建表（distributive_question + distributive_answer）
├── docs
│   ├── 乘法分配律-设计方案.md   # 设计与验收说明
│   ├── images/                 # 原型与验收截图
│   └── mockups/                # 静态视觉原型（不参与运行）
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
    │   │   ├── BountyAnswer.java             # 答题实体
    │   │   ├── DistributiveQuestion.java     # 分配律题目实体
    │   │   └── DistributiveAnswer.java       # 学生算式实体
    │   ├── mapper/
    │   │   ├── SysUserMapper.java
    │   │   ├── BountyQuestionMapper.java
    │   │   ├── BountyAnswerMapper.java
    │   │   ├── DistributiveQuestionMapper.java
    │   │   └── DistributiveAnswerMapper.java
    │   ├── service/
    │   │   └── CustomUserDetailsService.java # 数据库认证（status 禁用校验）
    │   ├── dto/
    │   │   └── DistributiveQuestionForm.java # 分配律新增/编辑表单 DTO
    │   ├── util/
    │   │   ├── XssUtil.java                  # 富文本 XSS 清理
    │   │   └── ExpressionValidator.java      # 分配律算式规范化 + 两种格式校验
    │   └── controller/
    │       ├── AdminController.java          # /index 首页
    │       ├── UserController.java           # /user/** 用户管理
    │       ├── BountyController.java         # /bounty/** 前台（大厅/答题/我的答题）
    │       ├── AdminBountyController.java    # /bounty/admin/** 题目管理/评分
    │       ├── DistributiveController.java   # /distributive/** 学习页 + 提交算式
    │       ├── AdminDistributiveController.java # /distributive/admin/** 题目管理 + 轮询
    │       ├── CustomErrorController.java    # 卡通错误页（403/404/500）
    │       └── HelloController.java          # /hello 测试页
    └── resources
        ├── application.yml
        ├── static/
        │   ├── css/admin.css                 # 后台 + 悬赏模块样式
        │   ├── css/distributive-learn.css    # 学生端 Pad 版式
        │   ├── css/distributive-admin.css    # 教师端科幻云海/甬道
        │   ├── js/rich-editor.js             # 轻量富文本编辑器
        │   ├── js/distributive-learn.js      # 学生端交互（符号键盘/校验/提交）
        │   └── js/distributive-admin.js      # 云海漂移/汇聚/轮询
        └── templates/
            ├── layout/common.html            # 侧边栏 + 顶部栏片段（按角色显示）
            ├── login.html / index.html / hello.html / error/
            ├── user_*.html                   # 用户管理页面
            ├── bounty/
            │   ├── hall.html                 # 悬赏大厅（公开）
            │   ├── answer.html               # 答题页
            │   ├── my_answers.html           # 我的答题列表
            │   ├── my_answer_detail.html     # 我的答题详情
            │   ├── admin_list.html           # 题目管理列表
            │   ├── admin_edit.html           # 添加/编辑题目
            │   └── admin_answers.html        # 答题情况 + 评分
            └── distributive/
                ├── learn.html                # 学习页（Pad 版式，公开）
                ├── admin_list.html           # 科幻云海 + 四条甬道
                └── admin_edit.html           # 新增/编辑题目（图片上传）
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

### distributive_question（分配律题目表）
| 字段 | 说明 |
|------|------|
| id | 主键 |
| title | 题目名称（编辑/预览/图片说明用，卡片不展示） |
| image_url | 图片链接（卡片与甬道唯一展示内容） |
| description | 详细说明（富文本，仅教师端编辑/预览使用，学生端不展示） |
| status | DRAFT草稿 / PUBLISHED已发布 |
| sort_order | 排序号，决定第几题与第几条甬道 |
| creator_id, publish_time | 创建人 / 首次发布时间 |
| deleted | 逻辑删除 |

### distributive_answer（学生算式表）
| 字段 | 说明 |
|------|------|
| id | 主键 |
| question_id | 所属题目 |
| expression | 规范化后的算式，如 `3*(4+5)` / `3*4+3*5` |
| create_time | 提交时间（甬道内按此排序） |
| deleted | 逻辑删除（教师可删乱填内容） |

## 访问地址

| 路径 | 权限 | 说明 |
|------|------|------|
| `/bounty/hall` | 公开 | 悬赏大厅（卡片式，按时间/星星排序，分页） |
| `/distributive/learn` | 公开 | 分配律学习页（Pad 版式，只展示已发布题目） |
| `/distributive/submit` | 公开 | 提交算式（POST，JSON） |
| `/login` | 公开 | 登录页（卡通风格） |
| `/hello` | 公开 | 测试页 |
| `/bounty/answer` | 登录 | 答题（GET表单 / POST提交，同一题只能答一次） |
| `/bounty/my-answers` | 登录 | 我的答题列表 |
| `/bounty/my-answer` | 登录 | 我的答题详情（题目+答案+评分） |
| `/index` | ADMIN | 后台首页 |
| `/user/list` | ADMIN | 用户管理（搜索/分页/增删改/禁用） |
| `/bounty/admin/list` | ADMIN/TEACHER | 题目管理列表（含答题统计） |
| `/bounty/admin/add` | ADMIN/TEACHER | 发布题目（富文本 + 星星选择器） |
| `/bounty/admin/edit` | ADMIN/TEACHER | 编辑题目 |
| `/bounty/admin/answers` | ADMIN/TEACHER | 查看答题 + 评分 |
| `/distributive/admin/list` | ADMIN/TEACHER | 分配律看板（一行四卡 + 云海 + 甬道，只展示图片） |
| `/distributive/admin/manage` | ADMIN/TEACHER | 题目管理（表格：编辑/发布/下架/删除） |
| `/distributive/admin/add` | ADMIN/TEACHER | 新增分配律题目 |
| `/distributive/admin/edit` | ADMIN/TEACHER | 编辑分配律题目 |
| `/distributive/admin/answers` | ADMIN/TEACHER | 轮询新算式（JSON，`?sinceId=`） |
| `/distributive/admin/answer/delete-json` | ADMIN/TEACHER | 删除单条算式（JSON，AJAX） |
| `/distributive/admin/answer/clear-json` | ADMIN/TEACHER | 擦除所有学生算式（逻辑删除，JSON） |
| `/logout` | 公开 | 退出（POST） |

## 运行前准备

### 1. 初始化数据库

```bash
mysql -uroot -p math < db/schema.sql                  # 用户表 + admin 初始化
mysql -uroot -p math < db/schema_bounty.sql           # 悬赏模块表
mysql -uroot -p math < db/schema_distributive.sql     # 分配律模块表
```

确认 `application.yml` 中 `spring.datasource` 配置正确。

### 2. 启动

```bash
mvn spring-boot:run
```

### 3. 访问

- http://localhost:8080/bounty/hall — 悬赏大厅（免登录）
- http://localhost:8080/distributive/learn — 分配律学习页（免登录，Pad 打开效果最佳）
- http://localhost:8080/login — 登录（admin/hxx123456 或自建用户）
- admin / teacher 登录后跳转 `/distributive/admin/list`（分配律管理），student 登录后跳转 `/distributive/learn`（分配律答题页）

## 关键实现说明

- **登录跳转**：`CustomAuthenticationSuccessHandler` 按角色跳转（ADMIN/TEACHER→`/distributive/admin/list`，STUDENT→`/distributive/learn`）。
- **禁用校验**：`CustomUserDetailsService` 用 `User.builder().disabled(status==0)`，`CustomAuthenticationFailureHandler` 捕获 `DisabledException` 跳转 `/login?disabled`。
- **富文本编辑器**：`static/js/rich-editor.js` 基于 `contenteditable + execCommand`，无外部依赖，存储为 HTML。
- **XSS 防护**：`XssUtil.clean()` 清理 script 标签、on* 事件属性、javascript: 协议。
- **题目预览**：控制器用正则去 HTML 标签生成纯文本预览（截断 100 字）。
- **分页**：MyBatis-Plus `PaginationInnerInterceptor`，列表页支持每页条数切换。
- **侧边栏**：`common.html` 用 `sec:authorize` 按角色显示菜单（STUDENT 看大厅/我的答题，ADMIN/TEACHER 额外看管理后台）。
- **学生提交免 CSRF**：`/distributive/submit` 是匿名公开接口，已在 `SecurityConfig` 中放行 CSRF（Pad 页面长时间停留导致 token 过期时会 403）；管理端接口仍校验 CSRF。
- **分配律算式校验**：`ExpressionValidator` 先做 NFKC 规范化（全角转半角、`×`→`*`、去空白），
  只放行 `a*(b+c)` 与 `a*b+a*c` 两种结构；展开式要求两个乘法有相同因数；错误提示按场景返回儿童化文案。
  前端 `distributive-learn.js` 用同一套规则即时提示，服务端兜底。
- **Pad 学习页**：`body.pad-shell` 只有卡片，无顶栏/标题/题号；卡片 `100vh` 铺满、`object-fit: contain` 统一画框，
  不同尺寸的图片都完整显示；答题层提供 0-9 / × / ( ) / + - 符号键盘（Pad 系统键盘没有 ×）；提交结果用居中弹框提示。
- **教师端云海**：气泡用「分槽铺开 + 每帧互相推挤」保证不重叠；甬道默认高度 0 不占位（卡片上没有任何按钮，题目增删改走「题目管理」页），
  点「开始归类」时先展开甬道（卡片被顶到顶部）、等过渡结束再统一测量落点，然后按 260ms 间隔一个一个飞入；飞行期间云海 z-index 提到最上层；
  落地变 chip（按算式格式分上下两栏），归类时卡片压暗去色、悬停恢复原色并轻微放大；删除算式走 AJAX，原地移除并底部轻提示，不刷新页面。
- **自动刷新**：`GET /distributive/admin/answers?sinceId=` 每 15 秒拉增量，页面隐藏时暂停；**归类状态下**新算式直接从顶部飞入对应甬道，不打断当前归类页面。

