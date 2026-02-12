# EasyCloudPan 快速启动指南

> 目标：让新手在 10 分钟内完成部署并访问系统

## 快速导航

| 场景 | 命令 | 访问地址 |
|------|------|----------|
| 本地开发 | `.\ops\local\startup.ps1` | http://localhost:8080 |
| Docker 部署 | `.\ops\docker\deploy_docker.ps1` | http://localhost:8080 |

> **提示**：Windows 用户推荐使用 PowerShell 脚本（`.ps1`），批处理脚本（`.bat`）也可用。

---

## 第一步：安装必要软件

在开始之前，请确保已安装以下软件：

| 软件 | 版本要求 | 下载地址 | 验证命令 |
|------|----------|----------|----------|
| JDK | 21+ | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/#java21) | `java -version` |
| Maven | 3.9+ | [Maven](https://maven.apache.org/download.cgi) | `mvn -version` |
| Node.js | 20+ | [Node.js LTS](https://nodejs.org/) | `node -v` |
| Docker Desktop | 最新版 | [Docker Desktop](https://www.docker.com/products/docker-desktop/) | `docker --version` |

> **提示**：安装 Docker Desktop 后，确保 Docker Desktop 正在运行

### 验证环境

打开命令行，依次执行：

```bat
java -version
mvn -version
node -v
npm -v
docker --version
docker compose version
```

所有命令都应该正常输出版本信息，无报错。

---

## 第二步：获取代码

```bat
git clone <你的仓库地址> easyCloudPan
cd easyCloudPan
```

如果已有代码，直接进入项目目录即可。

---

## 第三步：选择部署方式

### 方式 A：本地开发（推荐新手）

#### 3.1 初始化环境（首次运行）

```powershell
.\ops\local\setup.ps1
```

脚本会自动：
- 检查工具链
- 安装前端依赖
- 编译后端项目
- 生成配置文件 `ops\docker\.env`

#### 3.2 一键启动

```powershell
.\ops\local\startup.ps1
```

启动后会自动打开浏览器访问 http://localhost:8080

#### 3.3 验证服务

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端 | http://localhost:8080 | 用户界面 |
| 后端 API | http://localhost:7090/api | 接口服务 |
| MinIO 控制台 | http://localhost:9001 | 对象存储管理 |

---

### 方式 B：Docker 全栈部署

#### 3.1 配置环境变量

```powershell
copy ops\docker\.env.example ops\docker\.env
```

#### 3.2 一键部署

```powershell
.\ops\docker\deploy_docker.ps1
```

#### 3.3 验证服务

```powershell
docker compose -f ops\docker\docker-compose.yml ps
```

或使用健康检查脚本：

```powershell
.\ops\tools\health_check.ps1
```

所有服务状态应为 `running` 或 `healthy`。

---

## 第四步：注册与登录

### 重要说明

系统注册需要邮箱验证码。默认配置使用占位值，**无法发送真实邮件**。

### 配置邮件服务（如需注册功能）

1. 编辑 `ops\docker\.env` 文件
2. 修改邮件配置：

```env
SPRING_MAIL_HOST=smtp.qq.com
SPRING_MAIL_PORT=465
SPRING_MAIL_USERNAME=你的QQ号@qq.com
SPRING_MAIL_PASSWORD=QQ邮箱授权码
```

> **获取QQ邮箱授权码**：QQ邮箱 -> 设置 -> 账户 -> POP3/SMTP服务 -> 生成授权码

3. 重启后端服务

---

## 常见问题

### Q1: 端口被占用怎么办？

默认端口：`5432, 6379, 8080, 7090, 9000, 9001`

解决方案：
1. 关闭占用端口的程序
2. 或修改 `ops\docker\docker-compose.yml` 中的端口映射

### Q2: 后端启动失败？

检查步骤：
```powershell
docker compose -f ops\docker\docker-compose.yml ps
```

确保 `postgres` 和 `redis` 状态为 `healthy`。

或运行健康检查：
```powershell
.\ops\tools\health_check.ps1
```

### Q3: 前端无法访问？

1. 确认后端已启动：访问 http://localhost:7090/api/actuator/health
2. 检查控制台是否有错误信息

### Q4: MinIO 上传失败？

检查 `minio-init` 容器是否成功执行：
```bat
docker compose -f ops\docker\docker-compose.yml logs minio-init
```

应看到 `minio bucket is ready` 输出。

---

## 停止服务

### 本地开发

直接关闭启动脚本打开的两个 PowerShell 窗口。

停止基础设施容器：
```powershell
docker compose -f ops\docker\docker-compose.yml down
```

### Docker 部署

```powershell
.\ops\docker\stop_docker.ps1
```

清空所有数据（危险操作）：
```powershell
.\ops\docker\stop_docker.ps1 -Volumes
```

---

## 下一步

- 查看完整文档：[README.md](README.md)
- Docker 部署详解：[docs/DOCKER_DEPLOY_GUIDE.md](docs/DOCKER_DEPLOY_GUIDE.md)
- 数据库迁移说明：[database/README.md](database/README.md)
