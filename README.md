# 课堂互动与问答系统

一个面向教育场景的课堂互动平台，支持师生问答、考勤管理、作业提交、考试评阅、课程投票、AI 智能助教等功能。

## 技术栈

### 后端
- **Spring Boot 3.2.1** - Java 17
- **Spring Security** - 安全认证
- **Spring WebSocket** - 实时通讯
- **MyBatis Plus** - ORM 框架
- **MySQL** - 主数据库
- **Redis** - 缓存
- **MongoDB** - 文档存储
- **JWT** - 身份认证
- **LangChain4j** - AI 集成

### 前端
- **Vue 3** + TypeScript
- **Vite** - 构建工具
- **Element Plus** - UI 组件库
- **Pinia** - 状态管理
- **Vue Router** - 路由管理
- **Axios** - HTTP 请求

## 功能模块

| 模块 | 说明 |
|------|------|
| 用户管理 | 注册、登录、个人信息维护 |
| 课程管理 | 课程创建、加入、查询 |
| 班级管理 | 班级成员管理 |
| 问答系统 | 提问、回答、点赞互动 |
| 投票系统 | 教师发起投票（匿名/实名、单选/多选），学生参与投票 |
| 考勤系统 | 课堂签到、考勤统计 |
| 作业系统 | 作业发布、提交、批改 |
| 考试系统 | 考试创建、自动发布通知、学生作答、教师评阅 |
| 文件管理 | 课程资料上传下载 |
| 积分系统 | 课堂表现积分、排行榜 |
| AI 助手 | AI 智能问答 |
| 抽奖点名 | 随机抽取学生 |

## 最近更新（2026-04）

- 新增课程内投票功能：支持匿名/实名开关、单选/多选模式。
- 修复课程资料下载链路：前端优先获取后端签名下载链接，提升云存储下载稳定性。
- 优化头像/课程封面展示：优先使用数据库存储值并在后端统一解析可访问 URL。
- 考试流程优化：创建考试后自动发布通知，学生端按时间状态限制进入。
- 控制台统计优化：按角色展示不同指标，学生出勤率基于个人可参与签到统计。

## 项目结构

```
classroom/                 # 后端项目
├── src/main/java/com/classroom/
│   ├── config/           # 配置类
│   ├── controller/       # 控制器
│   ├── dto/              # 数据传输对象
│   ├── entity/          # 实体类
│   ├── exception/        # 异常处理
│   ├── repository/       # 数据访问层
│   ├── security/         # 安全认证
│   ├── service/         # 业务逻辑
│   └── vo/              # 视图对象

frontend/                 # 前端项目
├── src/
│   ├── api/             # API 接口
│   ├── assets/          # 静态资源
│   ├── components/      # 公共组件
│   ├── router/          # 路由配置
│   ├── stores/          # 状态管理
│   ├── utils/           # 工具函数
│   └── views/           # 页面视图
```

## 快速开始

### 后端启动

1. 配置 MySQL、Redis、MongoDB 数据库
2. 修改 `classroom/src/main/resources/application.yml` 中的数据库配置
3. 初始化数据库（首次启动建议执行）：

```sql
SOURCE classroom/src/main/resources/sql/init.sql;
```

4. 如果是已有库升级，补充执行迁移脚本（按版本顺序）：

```sql
SOURCE classroom/src/main/resources/sql/migration/V20260328__split_user_tables.sql;
SOURCE classroom/src/main/resources/sql/migration/V20260407__add_vote_tables.sql;
SOURCE classroom/src/main/resources/sql/migration/V20260408__upgrade_vote_mode_and_anonymous.sql;
```

5. 执行以下命令：

```bash
cd classroom
mvn clean install
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`

### 数据库脚本说明

- `classroom/src/main/resources/sql/init.sql`：新库一键初始化（含核心表结构与示例数据）。
- `classroom/src/main/resources/sql/migration/*.sql`：增量升级脚本（用于已有库升级）。

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`

## API 文档

启动后端后访问 `http://localhost:8080/swagger-ui.html` 查看 API 文档

## 环境要求

- Java 17+
- Node.js 18+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- MongoDB 6.0+

## 常见构建问题排查

### 前端构建失败

#### 1. `npm ci` 报错：package.json 与 package-lock.json 不同步

错误示例：
```
npm error `npm ci` can only install packages when your package.json and package-lock.json or npm-shrinkwrap.json are in sync.
npm error Missing: @types/node@25.5.0 from lock file
```

**原因**：修改了 `package.json`（如新增依赖）后没有重新生成 `package-lock.json`。

**解决方案**：在 `frontend` 目录下执行：
```bash
npm install
```
然后将更新后的 `package-lock.json` 提交到版本库。

#### 2. `vue-tsc` 启动时崩溃：`Search string not found`

错误示例：
```
Search string not found: "/supportedTSExtensions = .*(?=;)/"
```

**原因**：`vue-tsc`（通过 `@volar/typescript`）需要对 TypeScript 内部代码进行补丁，当 `vue-tsc` 版本与 TypeScript 版本不兼容时会出现此错误。通常是 `package-lock.json` 过旧，锁定了不兼容的旧版 `vue-tsc`。

**解决方案**：重新生成 `package-lock.json`：
```bash
cd frontend
rm package-lock.json
npm install
```
然后将新的 `package-lock.json` 提交到版本库。

#### 3. TypeScript 类型错误

**原因**：源代码中存在 TypeScript 类型错误，导致 `vue-tsc` 编译失败。

**解决方案**：在 `frontend` 目录下运行类型检查，查看并修复错误：
```bash
npx vue-tsc --noEmit
```

## 许可证

MIT License

## GitHub Actions 自动构建并推送 Docker 镜像（Docker Hub）

在 GitHub 仓库 Settings → Secrets and variables → Actions 中配置：

- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`

随后，工作流 `.github/workflows/docker-publish.yml` 会在以下情况自动构建并推送镜像到 Docker Hub：

- push 到 `main` / `master` / `dev`
- push tag：`v*`（例如 `v1.0.0`）

推送的镜像：

- `${DOCKER_USERNAME}/classroom-backend`
- `${DOCKER_USERNAME}/classroom-frontend`

tag 策略默认包含：分支名、commit 短 SHA、tag 名。

### 使用 Docker Compose 启动（推荐）

- `docker-compose.yml`：数据库依赖（MySQL/Redis/MongoDB）
- `docker-compose.app.yml`：应用服务（backend/frontend，基于 Docker Hub 镜像）

启动全套：

```bash
docker compose -f docker-compose.yml -f docker-compose.app.yml up -d
```

可选：在根目录新建 `.env` 指定你的 Docker Hub 用户名（用于 compose 引用镜像）：

```env
DOCKER_USERNAME=你的DockerHub用户名
```

