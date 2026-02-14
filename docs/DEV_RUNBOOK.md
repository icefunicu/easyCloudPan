# EasyCloudPan Developer Runbook

本文档描述从"已启动状态"如何一键验证后端全功能。

## 1. 前置条件

- Docker Desktop 已启动
- 后端服务已启动（通过 `ops/local/startup.ps1`）
- 基础设施容器（PostgreSQL/Redis/MinIO）已 healthy

## 2. 环境变量配置

测试脚本需要账号凭据，通过环境变量提供（切勿在脚本或命令行写死密码）：

```powershell
# 设置测试账号（替换为真实值）
$env:EASYPAN_EMAIL = "your_test_email@example.com"
$env:EASYPAN_PASSWORD = "your_test_password"
```

管理员功能测试需要管理员账号：

```powershell
$env:EASYPAN_ADMIN_EMAIL = "admin@example.com"
$env:EASYPAN_ADMIN_PASSWORD = "admin_password"
```

## 3. 健康检查

### 3.1 基础设施健康检查

```powershell
# 检查 Docker 容器状态
docker compose -f ops/docker/docker-compose.yml ps

# 运行完整健康检查脚本
powershell -ExecutionPolicy Bypass -File ops/tools/health_check.ps1
```

预期输出：所有服务显示 `healthy`，脚本无 `FAIL`。

### 3.2 后端应用健康检查

```powershell
# 方式一：PowerShell
Invoke-WebRequest -Uri "http://localhost:7090/api/actuator/health" -UseBasicParsing | Select-Object -ExpandProperty StatusCode

# 方式二：使用验证脚本
powershell -ExecutionPolicy Bypass -File scripts/verify_health_check.ps1
```

预期结果：
- 状态码 `200`
- 脚本输出 `PASS`（status=UP 且含 db/redis 组件）

## 4. 一键冒烟测试

### 4.1 启动后端（带验证码调试模式）

本地自动化测试需要获取验证码，启动时添加 `-ExposeCaptcha` 参数：

```powershell
powershell -ExecutionPolicy Bypass -File ops/local/startup.ps1 -ExposeCaptcha
```

这会设置 `CAPTCHA_DEBUG_HEADER=true`，`/api/checkCode` 接口将在响应头 `X-EasyPan-CheckCode` 中返回验证码。

### 4.2 运行冒烟测试

```powershell
# 设置账号
$env:EASYPAN_EMAIL = "your_test_email@example.com"
$env:EASYPAN_PASSWORD = "your_test_password"

# 运行冒烟测试
powershell -ExecutionPolicy Bypass -File scripts/api_smoke_test.ps1
```

预期输出：`ALL TESTS PASSED`

覆盖项：健康检查、登录、上传、列表、新建目录、重命名、下载闭环、删除到回收站、回收站操作、分享、永久删除、登出。

## 5. 后端单元测试

```powershell
cd backend
mvn test
```

预期输出：`BUILD SUCCESS`

## 6. 分模块验证脚本

### 6.1 基础设施验证

```powershell
# Flyway 迁移验证
powershell -ExecutionPolicy Bypass -File scripts/verify_flyway_migrations.ps1

# Redis 读写验证
powershell -ExecutionPolicy Bypass -File scripts/verify_redis_rw.ps1

# MinIO 读写验证
powershell -ExecutionPolicy Bypass -File scripts/verify_minio_rw.ps1
```

### 6.2 认证模块验证

```powershell
# 登录双 Token
powershell -ExecutionPolicy Bypass -File scripts/verify_login_dual_token.ps1

# Refresh Token
powershell -ExecutionPolicy Bypass -File scripts/verify_refresh_token.ps1

# 登出黑名单
powershell -ExecutionPolicy Bypass -File scripts/verify_logout_blacklist.ps1

# 获取用户信息
powershell -ExecutionPolicy Bypass -File scripts/verify_get_user_info.ps1
```

### 6.3 文件模块验证

```powershell
# 新建目录
powershell -ExecutionPolicy Bypass -File scripts/verify_new_folder.ps1

# 上传小文件
powershell -ExecutionPolicy Bypass -File scripts/verify_upload_small_file.ps1

# 重命名文件
powershell -ExecutionPolicy Bypass -File scripts/verify_rename_file.ps1

# 下载闭环
powershell -ExecutionPolicy Bypass -File scripts/verify_download_roundtrip.ps1

# 删除到回收站
powershell -ExecutionPolicy Bypass -File scripts/verify_delete_to_recycle.ps1

# 恢复文件
powershell -ExecutionPolicy Bypass -File scripts/verify_recover_file.ps1

# 永久删除
powershell -ExecutionPolicy Bypass -File scripts/verify_permanent_delete.ps1
```

### 6.4 分享模块验证

```powershell
# 创建/取消分享
powershell -ExecutionPolicy Bypass -File scripts/verify_share_file.ps1

# 分享访客链路
powershell -ExecutionPolicy Bypass -File scripts/verify_share_visitor.ps1
```

### 6.5 管理端验证

```powershell
# 需要管理员账号
$env:EASYPAN_EMAIL = "admin@example.com"
$env:EASYPAN_PASSWORD = "admin_password"

powershell -ExecutionPolicy Bypass -File scripts/verify_admin.ps1
```

覆盖项：M-ADMIN-001~006（系统设置、用户列表、用户启停、空间调整、文件管理、管理员下载）

### 6.6 观测模块验证

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify_observability.ps1
```

覆盖项：M-OBS-001~004（Web Vitals 上报、统计、健康检查、Prometheus 指标）

## 7. 故障排查

### 7.1 日志位置

- 后端日志：`backend/file/logs/easypan.log`（按日期轮转）
- Docker 日志：`docker compose -f ops/docker/docker-compose.yml logs <service>`

### 7.2 关键数据库表

| 表名 | 用途 |
|------|------|
| `flyway_schema_history` | 数据库迁移记录 |
| `user_info` | 用户信息 |
| `file_info` | 文件元数据 |
| `file_share` | 分享记录 |
| `email_code` | 邮箱验证码 |
| `share_access_log` | 分享访问日志 |
| `web_vitals_metrics` | 前端性能指标 |

### 7.3 常见问题

#### 问题：登录失败 "验证码错误"

**原因**：后端未以 `-ExposeCaptcha` 模式启动，脚本无法获取验证码。

**解决**：
```powershell
# 重启后端
powershell -ExecutionPolicy Bypass -File ops/local/startup.ps1 -ExposeCaptcha
```

#### 问题：MinIO 连接失败

**原因**：MinIO 容器未启动或 bucket 未创建。

**解决**：
```powershell
docker compose -f ops/docker/docker-compose.yml up -d minio minio-init
```

#### 问题：Redis 连接超时

**原因**：Redis 容器未启动或密码不匹配。

**解决**：
1. 检查容器状态：`docker compose -f ops/docker/docker-compose.yml ps redis`
2. 检查密码配置：`ops/docker/.env` 中 `REDIS_PASSWORD`

#### 问题：数据库迁移失败

**原因**：Flyway 检测到 checksum 不匹配。

**解决**：
1. 检查 `flyway_schema_history` 表
2. 开发环境可清空该表后重新启动

### 7.4 Redis 关键 Key

| Key 前缀 | 用途 |
|----------|------|
| `easypan:refresh:token:*` | Refresh Token 存储 |
| `easypan:jwt:blacklist:*` | JWT 黑名单 |
| `easypan:download:*` | 下载码 |
| `easypan:sys:*` | 系统设置缓存 |
| `easypan:user:space:*` | 用户空间使用缓存 |

## 8. 完整验证流程

从零开始验证后端全功能：

```powershell
# 1. 启动基础设施
docker compose -f ops/docker/docker-compose.yml up -d

# 2. 启动后端（调试模式）
powershell -ExecutionPolicy Bypass -File ops/local/startup.ps1 -ExposeCaptcha

# 3. 设置测试账号
$env:EASYPAN_EMAIL = "your_test_email@example.com"
$env:EASYPAN_PASSWORD = "your_test_password"

# 4. 运行健康检查
powershell -ExecutionPolicy Bypass -File scripts/verify_health_check.ps1

# 5. 运行冒烟测试
powershell -ExecutionPolicy Bypass -File scripts/api_smoke_test.ps1

# 6. 运行单元测试
cd backend; mvn test; cd ..
```

## 9. 验收矩阵参考

详细验收项请参考 [acceptance_matrix.md](./acceptance_matrix.md)。
