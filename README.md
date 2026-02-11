# EasyCloudPan

![Java](https://img.shields.io/badge/Java-21%2B-orange)
![Node.js](https://img.shields.io/badge/Node.js-20%2B-green)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F)
![Vue](https://img.shields.io/badge/Vue-3-42B883)

EasyCloudPan 是一个前后端分离的网盘系统，提供本地开发一键启动和 Docker 全栈部署能力，数据库结构由 Flyway 管理。

## 目录

- [功能概览](#功能概览)
- [技术栈](#技术栈)
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [启动脚本说明](#启动脚本说明)
- [配置说明](#配置说明)
- [项目结构](#项目结构)
- [文档导航](#文档导航)
- [常见问题](#常见问题)

## 功能概览

- 用户注册/登录（邮箱验证码、QQ 登录）
- 文件上传、管理、回收站
- 文件分享与转存
- 对象存储（MinIO）集成
- 基于 Flyway 的数据库版本迁移

## 技术栈

- 后端：Java 21, Spring Boot, MyBatis-Flex, Flyway
- 前端：Vue 3, Vite, Element Plus
- 基础设施：PostgreSQL 15, Redis 7, MinIO
- 容器编排：Docker Compose

## 环境要求

- JDK 21+
- Maven 3.9+
- Node.js 20+（建议 22）
- Docker Desktop（需包含 `docker compose`）

建议先执行：

```bat
java -version
javac -version
mvn -version
node -v
npm -v
docker --version
docker compose version
```

## 快速开始

### 方式 A：本地开发（推荐）

```bat
ops\local\setup.bat
ops\local\startup.bat
```

默认访问地址：

- 前端：`http://localhost:8080`
- 后端：`http://localhost:7090/api`
- MinIO Console：`http://localhost:9001`

### 方式 B：Docker 全栈部署

```bat
copy ops\docker\.env.example ops\docker\.env
ops\docker\deploy_docker.bat
```

停止服务：

```bat
ops\docker\stop_docker.bat
```

## 启动脚本说明

| 脚本 | 用途 | 常用参数 |
| --- | --- | --- |
| `ops\local\setup.bat` | 本地环境初始化、依赖检查、生成 `application-local.properties` | `--force`, `--skip-npm` |
| `ops\local\startup.bat` | 本地开发一键启动（依赖容器 + 后端 + 前端） | `--no-browser` |
| `ops\docker\deploy_docker.bat` | Docker 全栈部署 | `--no-build` |
| `ops\docker\stop_docker.bat` | 停止 Docker 服务 | `--volumes` |

查看脚本参数说明：

```bat
ops\local\setup.bat --help
ops\local\startup.bat --help
ops\docker\deploy_docker.bat --help
ops\docker\stop_docker.bat --help
```

## 配置说明

### 本地配置文件

- 路径：`backend/src/main/resources/application-local.properties`
- 由 `ops/local/setup.bat` 自动生成
- 如需覆盖默认配置，执行：`ops\local\setup.bat --force`

### Docker 环境变量

- 模板文件：`ops/docker/.env.example`
- 自定义文件：`ops/docker/.env`

常用变量：

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `REDIS_PASSWORD`
- `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, `MINIO_BUCKET`
- `QQ_APP_ID`, `QQ_APP_KEY`
- `SPRING_MAIL_PASSWORD`

## 项目结构

```text
easyCloudPan/
├── backend/                    # Spring Boot 后端
├── frontend/                   # Vue 前端
├── database/                   # 数据库迁移说明（不存放初始化 SQL）
├── docs/                       # 补充文档
├── scripts/                    # 运维/校验脚本
└── ops/                        # 部署与启动脚本
    ├── local/                  # 本地开发脚本
    ├── docker/                 # Docker 部署脚本与 compose
    ├── lib/                    # 脚本公共函数
    └── tools/                  # 辅助工具脚本
```

## 文档导航

- 本地从 0 启动：`QUICK_START.md`
- Docker 部署教程：`docs/DOCKER_DEPLOY_GUIDE.md`
- 数据库迁移说明：`database/README.md`
- 运维目录说明：`ops/README.md`
- 协作规范：`AGENTS.md`

## 常见问题

### 端口冲突

默认端口为 `5432/6379/8080/7090/9000/9001`。如被占用，请释放端口或调整 `ops/docker/docker-compose.yml`。

### 后端无法启动

优先检查：

```bat
docker compose ps
```

并确认 `ops/docker/.env` 中数据库与 Redis 配置正确。

### 邮箱验证码不可用

`ops\local\setup.bat` 生成的是占位配置。若要实际发信，请在 `ops/docker/.env` 中配置真实 `SPRING_MAIL_PASSWORD` 并重启后端。
