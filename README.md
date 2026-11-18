# 数学教学网站 (math-website)

基于 **Spring Boot + MySQL + Maven + Spring Security + Thymeleaf + MyBatis-Plus** 的数学教学网站基础工程。

## 技术栈

| 组件 | 版本 / 说明 |
|------|------------|
| Spring Boot | 2.7.18 |
| Spring Security | 随 Boot 版本（数据库认证 + BCrypt 加密） |
| Thymeleaf | 随 Boot 版本（含 springsecurity5 集成） |
| MyBatis-Plus | 3.5.5（数据访问层，含分页插件） |
| MySQL | 本地连接（驱动 com.mysql.cj.jdbc.Driver） |
| Maven | 构建工具 |
| Hutool | 5.8.25（工具包） |
| Lombok | 可选（简化实体类） |
| Java | 1.8+（本机实测 Java 21 可用） |

## 目录结构

```
math/
├── pom.xml
├── README.md
├── db
│   └── schema.sql                # sys_user 建表 + admin 初始化数据（BCrypt 密文）
└── src
    ├── main
    │   ├── java/com/math
    │   │   ├── MathApplication.java        # 主启动类（@MapperScan）
    │   │   ├── config
    │   │   │   ├── SecurityConfig.java     # Security 配置（数据库认证 + 角色权限）
    │   │   │   └── MybatisPlusConfig.java  # MyBatis-Plus 配置（分页插件）
    │   │   ├── entity
    │   │   │   └── SysUser.java            # 用户实体（含 role 字段）
    │   │   ├── mapper
    │   │   │   └── SysUserMapper.java      # 用户 Mapper（BaseMapper）
    │   │   ├── service
    │   │   │   └── CustomUserDetailsService.java  # 数据库认证服务
    │   │   └── controller
    │   │       ├── AdminController.java    # 需 ADMIN：/index
    │   │       ├── UserController.java     # 需 ADMIN：/user/list、/user/add
    │   │       └── HelloController.java    # 免认证：/hello、/login(GET)
    │   └── resources
    │       ├── application.yml
    │       ├── mapper/                     # Mapper XML（按需，目前空）
    │       └── templates
    │           ├── login.html              # 登录页（卡通风格）
    │           ├── index.html              # 后台首页（需 ADMIN）
    │           ├── user_list.html          # 用户列表（需 ADMIN）
    │           ├── user_add.html           # 添加用户（需 ADMIN）
    │           └── hello.html              # 测试页（免登录）
    └── test/java/com/math
        ├── MathApplicationTests.java       # 上下文加载测试
        └── PasswordHashTest.java           # 生成 BCrypt 哈希的临时工具
```

## 认证与角色

认证方式：**数据库认证**（已从内存切换）。用户信息存储在 `sys_user` 表，密码以 BCrypt 加密密文存储。

| 用户名 | 密码 | 角色 | 来源 |
|--------|------|------|------|
| admin  | hxx123456 | ADMIN | db/schema.sql 初始化 |

角色说明：
- **ADMIN**：可访问后台首页 `/index` 与用户管理 `/user/**`，可添加用户并赋予角色。
- **USER**：普通用户角色（后续可用于受限页面访问）。

新增用户：登录后访问 `/user/list` → 点击"添加用户"，填写用户名/密码/昵称/角色，密码会 BCrypt 加密后入库。

## 访问地址

| 路径 | 权限 | 说明 |
|------|------|------|
| `/hello` | 公开 | 测试页面 |
| `/login` | 公开 | 登录页面（GET 展示 / POST 由 Security 处理认证） |
| `/index` | ADMIN | 后台首页，登录成功后跳转至此 |
| `/user/list` | ADMIN | 用户列表 |
| `/user/add` | ADMIN | 添加用户（GET 表单 / POST 保存） |
| `/logout` | 公开 | 退出登录（POST 表单提交） |

## 运行前准备

### 1. 初始化数据库

本地 MySQL 已创建 `math` 库。执行项目自带的建表脚本（含 `sys_user` 表 + admin 初始化数据，密码为 BCrypt 密文）：

```bash
mysql -uroot -p math < db/schema.sql
```

或在 MySQL 客户端中粘贴 `db/schema.sql` 内容执行。

确认 `src/main/resources/application.yml` 中 `spring.datasource` 的用户名/密码与本地 MySQL 一致。

### 2. 确认本地环境

- JDK 1.8 及以上
- Maven 3.6+
- MySQL 5.7 / 8.0 / 9.x

### 3. 启动方式

```bash
mvn spring-boot:run
```
或打包运行：
```bash
mvn clean package -DskipTests
java -jar target/math-website-0.0.1-SNAPSHOT.jar
```

IDE：直接运行 `com.math.MathApplication` 的 main 方法。

### 4. 访问

- http://localhost:8080/hello  （免登录测试页）
- http://localhost:8080/index  （跳转登录页，输入 admin / hxx123456 后进入后台）

## 关键实现说明

- **数据库认证**：`CustomUserDetailsService`（@Service）实现 `UserDetailsService`，通过 `SysUserMapper` 查询 `sys_user`，用 `User.builder().roles(role)` 转换角色（Spring Security 自动加 `ROLE_` 前缀 → `ROLE_ADMIN` / `ROLE_USER`）。
- **密码加密**：`SecurityConfig` 注册 `BCryptPasswordEncoder`；添加用户时在 `UserController` 加密后入库；初始化 admin 密码哈希由 `PasswordHashTest` 生成后写入 `db/schema.sql`。
- **角色权限**：`SecurityConfig` 中 `.antMatchers("/index", "/user/**").hasRole("ADMIN")` 限制后台首页与用户管理仅 ADMIN 访问，其余需登录。
- **逻辑删除**：`SysUser.deleted` 用 `@TableLogic`，查询自动过滤已删除用户。

## 后续可扩展方向

- 为 USER 角色增加专属页面（如 `/home`），区分管理员与普通用户入口
- 用户编辑 / 删除 / 重置密码功能
- 题目管理、课程管理等业务模块（每个模块对应 entity / mapper / service / controller）
- Mapper XML（`resources/mapper/`）用于复杂 SQL
- 静态资源（CSS/JS）整合、统一异常页
