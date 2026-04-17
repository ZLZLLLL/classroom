# 云服务器部署指南

本文档说明如何在云服务器上使用 **阿里云容器镜像服务 (ACR)** 中的镜像，通过 Docker Compose 一键部署本项目。

---

## 前置条件

| 要求 | 版本 |
|------|------|
| Docker Engine | 24.0+ |
| Docker Compose Plugin | v2.20+ |
| 操作系统 | Linux (Ubuntu 22.04 / CentOS 8+ 推荐) |
| 开放端口 | 80（前端）、8080（后端 API，可选） |

> **安装 Docker（如尚未安装）**
> ```bash
> curl -fsSL https://get.docker.com | bash
> sudo usermod -aG docker $USER && newgrp docker
> ```

---

## 1. 登录阿里云容器镜像服务 (ACR)

```bash
docker login crpi-cbkazoyqsrh7o1rk.cn-shanghai.personal.cr.aliyuncs.com \
  -u <阿里云账号> \
  --password-stdin <<< "<ACR访问凭证>"
```

> **说明**
> - 账号为阿里云主账号用户名或 RAM 子账号。  
> - 访问凭证（密码）在 **阿里云控制台 → 容器镜像服务 → 访问凭证** 中生成。

登录成功提示：`Login Succeeded`

---

## 2. 获取项目文件

```bash
git clone https://github.com/ZLZLLLL/classroom.git
cd classroom
```

如服务器无法访问 GitHub，可仅上传 `docker-compose.prod.yml` 和 `.env` 文件。

---

## 3. 创建环境变量文件

在项目根目录创建 `.env` 文件，按需修改配置：

```bash
cat > .env <<'EOF'
# ── 镜像 Tag ────────────────────────────────────────────────
# 可用值：master | sha-<短SHA> | v1.0.0
IMAGE_TAG=master

# ── Spring 环境 ──────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=prod

# ── 数据库密码（生产环境请修改为强密码）────────────────────────
MYSQL_ROOT_PASSWORD=root123456
MYSQL_PASSWORD=classroom123
MONGO_PASSWORD=classroom123

# ── JVM 内存（按服务器 RAM 调整）────────────────────────────
JAVA_OPTS=-Xms256m -Xmx512m
EOF
```

> **安全提示**：请将 `.env` 加入 `.gitignore`，避免将密码提交到版本库。

---

## 4. 拉取最新镜像

```bash
# 拉取后端镜像
docker pull crpi-cbkazoyqsrh7o1rk.cn-shanghai.personal.cr.aliyuncs.com/classrooom/class_github_01:master

# 拉取前端镜像
docker pull crpi-cbkazoyqsrh7o1rk.cn-shanghai.personal.cr.aliyuncs.com/classrooom/class_github_frontend:master
```

若需要拉取特定版本，将 `master` 替换为对应的 tag（例如 `v1.0.0` 或 `sha-abc1234`）：

```bash
docker pull crpi-cbkazoyqsrh7o1rk.cn-shanghai.personal.cr.aliyuncs.com/classrooom/class_github_01:v1.0.0
```

---

## 5. 启动所有服务

```bash
# 首次启动（后台运行）
docker compose -f docker-compose.prod.yml up -d

# 查看启动日志
docker compose -f docker-compose.prod.yml logs -f
```

首次启动时 MySQL 初始化约需 30 秒，后端会等待数据库健康检查通过后再启动。

启动完成后访问：

| 服务 | 地址 |
|------|------|
| 前端页面 | `http://<服务器公网IP>` |
| 后端 API | `http://<服务器公网IP>:8080` |
| API 文档 | `http://<服务器公网IP>:8080/swagger-ui.html` |

---

## 6. 停止服务

```bash
# 停止但保留数据卷
docker compose -f docker-compose.prod.yml down

# 停止并删除数据卷（⚠ 会清空数据库数据）
docker compose -f docker-compose.prod.yml down -v
```

---

## 7. 更新到新版本

```bash
# 拉取新镜像
IMAGE_TAG=v1.1.0 docker compose -f docker-compose.prod.yml pull

# 滚动重启应用（不停数据库）
IMAGE_TAG=v1.1.0 docker compose -f docker-compose.prod.yml up -d --no-deps backend frontend
```

或在 `.env` 中修改 `IMAGE_TAG` 后执行：

```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

---

## 8. 常用运维命令

```bash
# 查看各容器运行状态
docker compose -f docker-compose.prod.yml ps

# 查看后端实时日志
docker compose -f docker-compose.prod.yml logs -f backend

# 查看前端实时日志
docker compose -f docker-compose.prod.yml logs -f frontend

# 进入后端容器（调试用）
docker exec -it classroom-backend bash

# 进入 MySQL（调试用）
docker exec -it classroom-mysql mysql -u classroom -p
```

---

## 9. 可选：暴露数据库端口（仅调试）

若需要从外部连接数据库，在 `docker-compose.prod.yml` 中取消对应 `ports` 段落的注释：

```yaml
# mysql 服务下添加：
ports:
  - "3306:3306"

# redis 服务下添加：
ports:
  - "6379:6379"

# mongodb 服务下添加：
ports:
  - "27017:27017"
```

> **安全提示**：生产环境不建议对公网暴露数据库端口，请配合云服务商的安全组/防火墙规则使用。

---

## 10. CI/CD 与自动化构建

本项目使用 GitHub Actions 自动构建并推送镜像到阿里云 ACR：

- 触发条件：push 到 `master` / `main` / `dev` 分支，或推送 `v*` tag
- 镜像 tag 策略：分支名、commit 短 SHA、version tag

所需 GitHub Secrets（在仓库 **Settings → Secrets → Actions** 中配置）：

| Secret 名称 | 说明 |
|-------------|------|
| `ALIYUN_USERNAME` | 阿里云账号 |
| `ALIYUN_PASSWORD` | ACR 访问凭证 |
| `DOCKER_USERNAME` | Docker Hub 账号（可选） |
| `DOCKER_PASSWORD` | Docker Hub 密码（可选） |

---

## 镜像地址速查

| 服务 | 阿里云 ACR 镜像 |
|------|----------------|
| 后端 (Spring Boot) | `crpi-cbkazoyqsrh7o1rk.cn-shanghai.personal.cr.aliyuncs.com/classrooom/class_github_01` |
| 前端 (Nginx + Vue) | `crpi-cbkazoyqsrh7o1rk.cn-shanghai.personal.cr.aliyuncs.com/classrooom/class_github_frontend` |
