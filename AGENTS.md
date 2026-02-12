# EasyCloudPan Agent 协作规范 (AGENTS.md)

## 1. 技术栈底座
- **JDK**: 21+ (必须支持 Virtual Threads)
- **Node.js**: 18+
- **DB**: PostgreSQL 15+
- **Migration**: Flyway (严禁手动修改本地 DB 结构而不同步脚本)
- **缓存**: Redis 7+ (分布式缓存) + Caffeine (本地缓存)
- **存储**: MinIO (兼容 S3 协议)
- **监控**: Prometheus + Grafana + Spring Boot Actuator

## 2. 编码规约 (Lombok 强制)
为了保持代码整洁，所有实体类遵循以下规范：
- **PO/DTO/VO**: 必须使用 Lombok `@Data` 或 `@Getter/@Setter` 注解，移除手动生成的样板代码。
- **字段命名**: 布尔类型字段避免以 `is` 开头（若必须，请手动配置属性名以确保兼容性，如 `private Boolean admin` 处理 `isAdmin` 逻辑）。
- **构造函数**: 优先使用 `@NoArgsConstructor` 和 `@AllArgsConstructor`。

## 3. 数据库变更规范
- 每一次数据库结构的修改，必须在 `backend/src/main/resources/db/migration` 下创建新的版本脚本（格式：`V[时间戳/版本]__描述.sql`）。
- **索引命名规范**: `idx_表名_字段1_字段2`（如 `idx_file_user_pid_del`）
- **复合索引顺序**: 遵循最左前缀原则，高选择性字段在前
- **部分索引**: 对于有明确过滤条件的查询，使用 `WHERE` 子句创建部分索引

## 4. Agent 协作流程 (System Spec)
- **计划先行**: Agent 在处理大型任务（涉及多文件修改）前，必须产出 `implementation_plan.md`。
- **用户确认**: 只有在用户确认后方可执行。
- **收尾清理**: 任务结束后，Agent 负责更新 `walkthrough.md`。

## 5. 项目结构认知
- `ops/local/setup.bat` 是环境一致性的唯一入口。
- `ops/docker/docker-compose.yml` 包含 Redis, MinIO, PostgreSQL 的标准联调环境。

## 6. 性能优化规范

### 6.1 数据库查询优化
- **禁止 N+1 查询**: 使用 JOIN 或批量查询替代循环查询
- **分页查询**: 优先使用游标分页（Cursor Pagination）而非 OFFSET 分页
- **查询字段**: 避免 `SELECT *`，只查询需要的字段
- **索引使用**: 确保 WHERE、ORDER BY、JOIN 字段都有索引覆盖

### 6.2 缓存使用规范
- **多级缓存**: L1 (Caffeine) → L2 (Redis) → Database
- **缓存 Key 命名**: `业务模块:功能:标识`（如 `user:space:userId123`）
- **缓存 TTL**: 根据数据更新频率设置合理的过期时间
  - 热数据：1 小时
  - 温数据：6 小时
  - 冷数据：1 天
  - 系统配置：30 分钟
- **缓存穿透保护**: 使用布隆过滤器或空值缓存
- **缓存雪崩保护**: 设置随机过期时间，避免同时失效

#### 缓存 Key 命名规范
| 业务场景 | Key 格式 | TTL | 示例 |
|----------|----------|-----|------|
| 用户信息 | `user:info:{userId}` | 1h | `user:info:12345` |
| 用户空间 | `user:space:{userId}` | 30min | `user:space:12345` |
| 文件信息 | `file:info:{fileId}` | 6h | `file:info:abc123` |
| 文件夹列表 | `folder:list:{userId}:{parentId}` | 5min | `folder:list:12345:root` |
| 系统配置 | `sys:config:{configKey}` | 30min | `sys:config:upload_limit` |
| Token 黑名单 | `token:blacklist:{jti}` | Token 有效期 | `token:blacklist:xyz789` |
| Refresh Token | `token:refresh:{userId}` | 7d | `token:refresh:12345` |

### 6.3 虚拟线程使用规范
- **适用场景**: I/O 密集型任务（数据库查询、文件操作、网络请求）
- **不适用场景**: CPU 密集型任务（加密、压缩、图像处理）
- **连接池配置**: 虚拟线程场景下适当增加连接池大小
- **监控**: 使用 `VirtualThreadMonitor` 监控虚拟线程数量

### 6.4 文件处理规范
- **大文件处理**: 使用流式处理，避免一次性加载到内存
- **分片上传**: 分片大小建议 5MB，最大并发 5 个
- **零拷贝**: 使用 NIO `FileChannel.transferTo()` 进行文件传输
- **异步处理**: 文件转码、缩略图生成等耗时操作使用 `@Async` 异步执行

### 6.5 前端性能规范
- **代码分割**: 使用 Vite manualChunks 按模块分割代码
- **图片优化**: 
  - 使用 vue3-lazy 懒加载图片
  - 图片压缩后上传
  - 使用 WebP 格式
- **打包优化**:
  - 启用 Terser 压缩
  - 启用 Brotli 压缩
  - CSS 代码分割
- **首屏优化**:
  - 路由懒加载
  - 骨架屏占位
  - 关键 CSS 内联

## 7. 安全编码规范

### 7.1 认证授权
- **JWT Token**: 使用双 Token 机制（Access Token 15 分钟 + Refresh Token 7 天）
- **Token 黑名单**: 登出时必须将 Token 加入黑名单
- **权限验证**: 所有文件操作 API 必须验证用户权限
- **敏感操作**: 管理员操作必须二次验证

#### JWT Token 安全规范
| 规范项 | 要求 | 说明 |
|--------|------|------|
| Access Token 有效期 | 15 分钟 | 短期有效，降低泄露风险 |
| Refresh Token 有效期 | 7 天 | 长期有效，存储于 Redis |
| Token 存储 | Redis | 支持主动失效 |
| 黑名单检查 | 每次请求 | 在 JwtAuthenticationFilter 中检查 |
| Token 刷新 | 滑动刷新 | 每次使用 Refresh Token 生成新的 Access Token |

### 7.2 数据安全
- **配置加密**: 敏感配置使用 Jasypt 加密存储
- **密码存储**: 使用 BCrypt 强度 12 加密
- **SQL 注入**: 使用参数化查询，禁止字符串拼接 SQL
- **XSS 防护**: 前端输出时进行 HTML 转义

#### 敏感配置加密清单
| 配置项 | 加密要求 | 环境变量 |
|--------|----------|----------|
| JWT 密钥 | 必须加密 | `JWT_SECRET` |
| 数据库密码 | 必须加密 | `POSTGRES_PASSWORD` |
| Redis 密码 | 必须加密 | `SPRING_DATA_REDIS_PASSWORD` |
| MinIO 密钥 | 必须加密 | `MINIO_SECRET_KEY` |
| QQ 登录密钥 | 必须加密 | `QQ_APP_KEY` |
| 邮箱密码 | 必须加密 | `SPRING_MAIL_PASSWORD` |

### 7.3 文件安全
- **类型校验**: 基于文件内容（Magic Number）而非扩展名
- **大小限制**: 单文件最大 15MB（可配置）
- **路径遍历**: 禁止使用用户输入构造文件路径
- **访问控制**: 文件下载前必须验证用户权限

#### 文件类型白名单
| 类型 | Magic Number | 允许扩展名 |
|------|--------------|------------|
| 图片 | FF D8 FF (JPEG), 89 50 4E 47 (PNG) | .jpg, .jpeg, .png, .gif, .webp |
| 文档 | 25 50 44 46 (PDF), D0 CF 11 E0 (DOC) | .pdf, .doc, .docx, .xls, .xlsx |
| 视频 | 00 00 00 xx 66 74 79 70 (MP4) | .mp4, .avi, .mov |
| 音频 | 49 44 33 (MP3), 66 4C 61 43 (FLAC) | .mp3, .wav, .flac |

### 7.4 分享安全
- **密码保护**: 分享链接可设置访问密码
- **过期时间**: 分享链接必须设置过期时间
- **访问日志**: 记录所有分享链接访问记录
- **次数限制**: 可设置最大访问次数

## 8. 监控和日志规范

### 8.1 日志规范
- **日志级别**:
  - ERROR: 系统错误，需要立即处理
  - WARN: 警告信息，需要关注
  - INFO: 重要业务操作（登录、上传、删除）
  - DEBUG: 调试信息（开发环境）
- **结构化日志**: 使用 JSON 格式，包含 userId、traceId、timestamp
- **敏感信息脱敏**: 密码、Token、手机号等必须脱敏
- **日志采样**: 高频日志（如心跳）使用采样策略

#### 日志格式规范
```json
{
  "timestamp": "2024-01-15T10:30:00.000Z",
  "level": "INFO",
  "logger": "com.easypan.service.FileInfoService",
  "message": "File uploaded successfully",
  "traceId": "abc123def456",
  "userId": "12345",
  "context": {
    "fileId": "file789",
    "fileName": "document.pdf",
    "fileSize": 1024000
  }
}
```

#### 敏感信息脱敏规则
| 字段类型 | 脱敏规则 | 示例 |
|----------|----------|------|
| 密码 | 完全隐藏 | `******` |
| Token | 保留前 6 位 | `eyJhbG******` |
| 手机号 | 保留前 3 后 4 位 | `138****5678` |
| 邮箱 | 保留前 2 字符和域名 | `ab***@example.com` |
| 身份证 | 保留前 3 后 4 位 | `110***********1234` |

### 8.2 监控指标
- **API 性能**: 响应时间、QPS、错误率
- **数据库**: 慢查询、连接池使用率、事务耗时
- **缓存**: 命中率、内存使用、过期 Key 数量
- **业务指标**: 文件上传成功率、用户活跃度、存储空间使用

#### Prometheus 指标命名规范
| 指标类型 | 命名格式 | 示例 |
|----------|----------|------|
| 计数器 | `模块_操作_total` | `easypan_file_upload_total` |
| 直方图 | `模块_操作_duration_seconds` | `easypan_api_request_duration_seconds` |
| 仪表盘 | `模块_资源_current` | `easypan_cache_hit_rate` |
| 摘要 | `模块_操作_summary` | `easypan_db_query_summary` |

### 8.3 告警规则
- **P0 告警**: API 错误率 > 1%、数据库连接池耗尽
- **P1 告警**: API P95 响应时间 > 1s、缓存命中率 < 80%
- **P2 告警**: 磁盘使用率 > 80%、内存使用率 > 85%

#### 告警分级处理
| 级别 | 响应时间 | 处理方式 | 通知渠道 |
|------|----------|----------|----------|
| P0 | < 5 分钟 | 立即处理，必要时回滚 | 电话 + 短信 + 邮件 |
| P1 | < 30 分钟 | 当日内处理 | 短信 + 邮件 |
| P2 | < 4 小时 | 计划内处理 | 邮件 |

### 8.4 Grafana 仪表板
- **系统概览仪表板**: CPU、内存、磁盘、网络、JVM 状态
- **性能监控仪表板**: API 响应时间、数据库查询、缓存命中率
- **业务指标仪表板**: 文件上传、用户活跃度、存储空间

## 9. 测试规范

### 9.1 单元测试
- **覆盖率要求**: 核心业务逻辑 > 90%，整体代码 > 70%
- **测试命名**: `test方法名_场景_期望结果`（如 `testUploadFile_SecondUpload_Success`）
- **Mock 使用**: 外部依赖（数据库、Redis、MinIO）使用 Mock
- **断言**: 使用 AssertJ 提供更清晰的断言

### 9.2 属性测试
- **使用场景**: 验证通用属性（如文件完整性、权限隔离）
- **框架**: 使用 jqwik 进行属性测试
- **生成器**: 编写智能生成器，约束输入空间

### 9.3 性能测试
- **基准测试**: 每次优化后进行性能对比
- **压力测试**: 模拟 1000+ 并发用户
- **稳定性测试**: 长时间运行（24 小时）验证内存泄漏

## 10. 部署和运维规范

### 10.1 环境配置
- **开发环境**: 使用 `ops/local/setup.bat` 初始化
- **测试环境**: 使用 Docker Compose 部署
- **生产环境**: 使用 Kubernetes 或 Docker Swarm

### 10.2 配置管理
- **环境变量**: 敏感配置通过环境变量传递
- **配置中心**: 生产环境使用 Spring Cloud Config
- **配置审计**: 记录所有配置变更

### 10.3 发布流程
- **代码审查**: 所有代码必须经过 Code Review
- **自动化测试**: CI 流程必须通过所有测试
- **灰度发布**: 生产环境使用灰度发布策略
- **回滚方案**: 每次发布前准备回滚方案
