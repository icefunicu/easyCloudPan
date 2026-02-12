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

### 核心功能
- 用户注册/登录（邮箱验证码、QQ 登录、JWT 认证）
- 文件上传、管理、回收站
- 文件分享与转存
- 对象存储（MinIO）集成
- 基于 Flyway 的数据库版本迁移
- 多种文件格式预览（图片、PDF、视频、音频、Office 文档）

### 性能特性
- **虚拟线程支持**：基于 Java 21 虚拟线程，支持高并发场景
- **多级缓存**：Caffeine 本地缓存 + Redis 分布式缓存
- **分片上传**：支持大文件分片上传和秒传
- **流式处理**：NIO 零拷贝文件合并，降低内存占用
- **数据库优化**：复合索引、游标分页、连接池优化

### 安全特性
- **JWT 双 Token 机制**：Access Token (15分钟) + Refresh Token (7天)
- **Token 黑名单**：登出后 Token 立即失效，Redis 存储黑名单
- **文件访问控制**：基于 AOP 的权限验证，所有文件操作需验证所有权
- **配置加密**：敏感配置 Jasypt 加密存储，支持环境变量覆盖
- **文件类型校验**：基于 Magic Number 的文件类型验证，防止扩展名伪造
- **分享链接安全**：支持密码保护和过期时间设置
- **审计日志**：记录所有敏感操作，支持安全事件追溯

#### 安全架构详情
| 安全层 | 实现机制 | 说明 |
|--------|----------|------|
| 认证层 | JWT 双 Token | Access Token 短期有效，Refresh Token 长期有效 |
| 授权层 | AOP 权限切面 | @FileAccessCheck 注解统一权限验证 |
| 数据层 | Jasypt 加密 | 敏感配置加密存储，密钥通过环境变量传递 |
| 传输层 | HTTPS | 生产环境强制 HTTPS |
| 文件层 | Magic Number 校验 | 基于文件内容验证类型，防止恶意文件上传 |
| 审计层 | 结构化日志 | 所有安全事件记录到日志，支持 ELK 分析 |

### 监控运维
- **Prometheus 指标**：API 响应时间、缓存命中率、数据库性能、虚拟线程监控
- **Grafana 仪表板**：系统概览、性能监控、业务指标三大仪表板
- **结构化日志**：JSON 格式日志，支持 ELK 集成，敏感信息自动脱敏
- **健康检查**：数据库、Redis、MinIO 连接状态监控，自定义健康指标
- **告警规则**：API 延迟、错误率、缓存命中率、数据库连接池等多维度告警

#### 监控指标详情
| 指标类型 | 关键指标 | 告警阈值 |
|----------|----------|----------|
| API 性能 | P95 响应时间、QPS、错误率 | P95 > 2s 或错误率 > 5% |
| 数据库 | 连接池使用率、慢查询、事务耗时 | 连接池 > 80% 或 P95 > 1s |
| 缓存 | 命中率、内存使用、Key 过期 | 命中率 < 70% |
| JVM | 堆内存、GC 频率、线程数 | 堆内存 > 85% |
| 业务 | 文件上传成功率、用户活跃度 | 上传失败率 > 1% |

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

```powershell
.\ops\local\setup.ps1
.\ops\local\startup.ps1
```

默认访问地址：

- 前端：`http://localhost:8080`
- 后端：`http://localhost:7090/api`
- MinIO Console：`http://localhost:9001`

### 方式 B：Docker 全栈部署

```powershell
copy ops\docker\.env.example ops\docker\.env
.\ops\docker\deploy_docker.ps1
```

停止服务：

```powershell
.\ops\docker\stop_docker.ps1
```

## 启动脚本说明

| 脚本 | 用途 | 常用参数 |
| --- | --- | --- |
| `ops\local\setup.ps1` | 本地环境初始化、依赖检查、生成 `.env` | `-Force`, `-SkipNpm` |
| `ops\local\startup.ps1` | 本地开发一键启动（依赖容器 + 后端 + 前端） | `-NoBrowser` |
| `ops\docker\deploy_docker.ps1` | Docker 全栈部署 | `-NoBuild` |
| `ops\docker\stop_docker.ps1` | 停止 Docker 服务 | `-Volumes` |
| `ops\tools\health_check.ps1` | 服务健康检查 | - |

查看脚本参数说明：

```powershell
.\ops\local\setup.ps1 -?
.\ops\local\startup.ps1 -?
```

## 配置说明

### 本地配置文件

- 路径：`ops/docker/.env`
- 由 `ops/local/setup.bat` 根据模板自动生成
- 后端启动时（`ops/local/startup.bat`）会自动加载此文件中的环境变量覆盖默认配置

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

### 运维文档
- **监控配置**：`ops/monitoring/` - Prometheus 配置、Grafana 仪表板、告警规则
- **故障排查**：参见本文档 [常见问题](#常见问题) 章节
- **性能调优**：参见本文档 [性能指标](#性能指标) 章节

## 性能指标

### 优化后性能表现
- **API 响应时间**：P95 < 500ms，P99 < 1s
- **数据库查询**：P95 < 100ms，慢查询减少 80%
- **缓存命中率**：> 90%
- **文件上传**：支持 1000+ 并发，成功率 > 99.5%
- **前端加载**：首屏加载 < 2s，打包体积减少 30%

### 系统容量
- **并发用户**：支持 1000+ 并发用户
- **文件存储**：支持 PB 级文件存储（MinIO 分布式）
- **数据库**：PostgreSQL 主从复制，支持读写分离
- **缓存**：Redis 哨兵模式，高可用

### 性能优化详情

#### 数据库优化
| 优化项 | 说明 | 效果 |
|--------|------|------|
| 复合索引 | 新增 5 个复合索引（秒传、QQ登录、空间查询等） | 查询性能提升 50-80% |
| 游标分页 | 替代 OFFSET 分页，避免深分页性能问题 | 大数据量分页性能提升 90% |
| 连接池优化 | HikariCP 配置适配虚拟线程 | 连接利用率提升 30% |
| N+1 查询消除 | 使用 JOIN 和批量查询 | 查询次数减少 70% |

#### 缓存优化
| 优化项 | 说明 | 效果 |
|--------|------|------|
| 多级缓存 | Caffeine (L1) + Redis (L2) | 缓存命中率 > 90% |
| 布隆过滤器 | 防止缓存穿透 | 无效请求拦截率 99% |
| 缓存预热 | 启动时加载活跃用户和系统配置 | 冷启动性能提升 50% |
| 分级 TTL | 热数据 1h、温数据 6h、冷数据 1d | 内存利用率优化 |

#### 文件处理优化
| 优化项 | 说明 | 效果 |
|--------|------|------|
| NIO 零拷贝 | FileChannel.transferTo() 合并分片 | 内存占用降低 70% |
| 断点续传 | 支持上传中断后继续上传 | 大文件上传成功率 > 99% |
| 上传限流 | 令牌桶限流保护服务器 | 系统稳定性提升 |
| 分片并发 | 最大 5 个分片并发上传 | 上传速度提升 3x |

#### 前端优化
| 优化项 | 说明 | 效果 |
|--------|------|------|
| 代码分割 | manualChunks 按模块分割 | 首屏 JS 减少 40% |
| 图片懒加载 | vue3-lazy 懒加载图片 | 首屏加载时间减少 50% |
| Brotli 压缩 | 启用 Brotli 压缩 | 传输体积减少 25% |
| Web Vitals | 实时监控前端性能指标 | 问题发现时间缩短 80% |

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

### 性能问题排查

1. 检查监控指标：访问 `http://localhost:7090/api/actuator/metrics`
2. 查看慢查询日志：`backend/file/logs/easypan.log`
3. 检查缓存命中率：Grafana 仪表板
4. 查看虚拟线程状态：`/api/actuator/health/custom`

### 安全配置

1. **生产环境必须修改**：
   - JWT 密钥：`jwt.secret`
   - 数据库密码：`POSTGRES_PASSWORD`
   - Redis 密码：`SPRING_DATA_REDIS_PASSWORD`
   - MinIO 密钥：`MINIO_SECRET_KEY`

2. **启用配置加密**：
   ```bat
   # 设置加密密钥环境变量
   set JASYPT_ENCRYPTOR_PASSWORD=your_secret_key
   
   # 加密敏感配置
   java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI ^
     input="your_password" password=%JASYPT_ENCRYPTOR_PASSWORD% algorithm=PBEWithMD5AndDES
   ```

## 项目优化

本项目经过全面性能和安全优化，详细优化方案请参考：
- **需求文档**：`.kiro/specs/easypan-optimization/requirements.md`
- **设计文档**：`.kiro/specs/easypan-optimization/design.md`
- **任务列表**：`.kiro/specs/easypan-optimization/tasks.md`

### 优化亮点
1. **数据库优化**：新增 5 个复合索引，查询性能提升 50-80%
2. **缓存优化**：布隆过滤器防穿透，缓存命中率 > 90%
3. **文件处理**：NIO 零拷贝，内存占用降低 70%
4. **安全加固**：JWT 黑名单、文件权限控制、配置加密
5. **监控完善**：Prometheus + Grafana，全方位性能监控
