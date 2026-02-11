# EasyCloudPan 本地从 0 启动教程（Windows）

目标：在一台全新本地机器上，把 EasyCloudPan 跑起来并可访问。

## 1. 前置环境

必须安装：
- JDK 21+
- Maven 3.9+
- Node.js 20+（建议 22）
- Docker Desktop（需启用 `docker compose`）

快速检查：
```bat
java -version
javac -version
mvn -version
node -v
npm -v
docker --version
docker compose version
```

期望结果：
- 上述命令全部可执行且无报错
- `javac` 主版本 >= 21
- `node` 主版本 >= 20

## 2. 获取项目代码

```bat
git clone <你的仓库地址> easyCloudPan
cd easyCloudPan
```

如果你已经在项目目录中，可以跳过这一步。

## 3. 初始化本地环境（只需执行一次）

```bat
ops\local\setup.bat
```

脚本会自动完成：
- 工具链检查（Java/Maven/Node/Docker）
- 安装前端依赖（`frontend/node_modules`）
- 生成本地配置：`ops/docker/.env`
- 创建本地文件目录：`backend/file/*`

可选参数：
- `ops\local\setup.bat --force`：覆盖已存在的本地配置文件
- `ops\local\setup.bat --skip-npm`：跳过前端依赖安装

## 4. 一键启动本地开发环境

```bat
ops\local\startup.bat
```

脚本会执行：
- 启动依赖容器：PostgreSQL、Redis、MinIO、minio-init
- 新开窗口启动后端（`mvn spring-boot:run -Dspring-boot.run.profiles=local`）
- 新开窗口启动前端（`npm run dev`）
- 自动打开浏览器 `http://localhost:8080`

可选参数：
- `ops\local\startup.bat --no-browser`：不自动打开浏览器

## 5. 启动结果验证（必须做）

1. 检查容器状态：
```bat
docker compose -f ops/docker/docker-compose.yml ps
```
期望看到 `postgres`、`redis`、`minio` 为 `running`，`minio-init` 为 `exited (0)`。

2. 检查后端健康检查：
```bat
curl http://localhost:7090/api/actuator/health
```
期望返回包含 `UP` 的 JSON。

3. 打开页面：
- 前端：`http://localhost:8080`
- 后端 API 基址：`http://localhost:7090/api`
- MinIO 控制台：`http://localhost:9001`

## 6. 首次登录/注册说明

- 系统注册流程需要发送邮箱验证码。
- `ops\local\setup.bat` 生成的 `ops/docker/.env` 默认是占位值（`SPRING_MAIL_PASSWORD=dummy_mail_password`）。
- 如果你要实际注册用户，请修改 `ops/docker/.env` 中邮件相关配置（尤其是 `SPRING_MAIL_PASSWORD`）后重启后端。

## 7. 停止服务

- 停止本地前后端：直接关闭 `ops\local\startup.bat` 打开的两个命令窗口。
- 停止基础容器：
```bat
docker compose -f ops/docker/docker-compose.yml down
```
- 如需清空数据（危险操作）：
```bat
docker compose -f ops/docker/docker-compose.yml down -v
```

## 8. 手动启动（不使用一键脚本）

```bat
docker compose -f ops/docker/docker-compose.yml up -d postgres redis minio minio-init
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

另开一个终端：
```bat
cd frontend
npm run dev
```

## 9. 常见问题

### 9.1 端口占用
- 默认端口：`5432/6379/8080/7090/9000/9001`
- 若冲突，先关闭占用进程，或修改 `ops/docker/docker-compose.yml` 对应端口后重启。

### 9.2 后端无法连数据库
- 先执行：`docker compose -f ops/docker/docker-compose.yml ps`
- 再检查 `ops/docker/.env` 中数据库配置是否为：
  - `POSTGRES_DB=easypan`
  - 用户名/密码与 `ops/docker/docker-compose.yml` 一致（默认 `postgres/123456`）

### 9.3 MinIO 上传失败
- 检查 `minio-init` 是否 `exited (0)`。
- 检查 `ops/docker/.env` 中：
  - `MINIO_ENDPOINT=http://minio:9000` (Docker internal) or `http://localhost:9000` (Local)
  - `MINIO_BUCKET_NAME=easypan`

## 10. Docker 全栈部署（可选）

```bat
copy ops\docker\.env.example ops\docker\.env
ops\docker\deploy_docker.bat
```

可选参数：
- `ops\docker\deploy_docker.bat --no-build`：跳过镜像构建，直接启动现有镜像

停止：
```bat
ops\docker\stop_docker.bat
```

如需停止并清理卷（危险操作）：
```bat
ops\docker\stop_docker.bat --volumes
```

详细说明见：`docs/DOCKER_DEPLOY_GUIDE.md`
