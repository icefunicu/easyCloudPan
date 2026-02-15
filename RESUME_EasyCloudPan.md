# EasyCloudPan（全栈网盘系统）简历版项目描述与亮点

> 生成时间：2026-02-15  
> 事实来源：仓库代码与配置（扫描产物索引见 `agent_runs/20260215-1800_full_scan/coverage_confirmation.md`）  
> 安全说明：本文不包含任何真实 `token/cookie/密码/私钥/.env 明文/连接串` 等敏感值。

## 一句话简介
前后端分离的网盘系统，覆盖账号认证、文件上传/管理/回收站、文件分享与访客访问、文件预览、对象存储（MinIO/S3）、可观测性与一键部署交付。

## 项目周期
**2024.04 - 至今** （持续迭代中）

## 技术栈
- 前端：Vue 3、Vite、TypeScript、Element Plus、Pinia、Axios、PWA、Playwright
- 后端：Java 21、Spring Boot 3、Spring Security、OAuth2 Client、MyBatis-Flex、Flyway、Redis、Caffeine、JJWT、Jasypt、Micrometer/Actuator
- 基础设施：PostgreSQL、Redis、MinIO（S3 兼容）、Nginx、Prometheus、Grafana、Docker Compose
- 工程化：本地一键启动脚本、Docker 全栈部署脚本、健康检查脚本、API 冒烟/验收脚本、前端 type-check/lint/e2e

## 核心能力与亮点（简历要点，可按实际承担选择/改写）
1. 分片上传与断点续传闭环：前端按 5MB 分片 + 并发窗口上传 + 失败重试/指数退避；后端提供已上传分片查询与上传进度接口，保证弱网可恢复与任务可观测。
2. 上传性能优化：后端使用 NIO `FileChannel.transferTo` 进行零拷贝合并，降低内存占用与拷贝开销；上传完成后通过事务提交后回调触发异步转码。
3. 秒传与去重：引入 Redis + Guava BloomFilter（启动预热 + 上传后回写），秒传判定先 `mightContain` 再精确查库，降低高频秒传场景的无效 DB 访问。
4. 上传安全治理：首片执行危险扩展名黑名单 + Magic Number 文件头校验（含 mp4/mov 特判），减少扩展名伪造与可执行文件上传风险。
5. 请求层稳定性：前端封装 Axios 拦截器，支持 `AbortController` 取消/去重请求；对 `901` 进行 refresh 队列化与请求重放，避免并发刷新风暴。
6. 多级缓存策略：实现 L1 Caffeine + L2 Redis + L3 DB 的多级缓存读取，并在转码/更新后主动驱逐缓存，降低状态陈旧窗口。
7. 虚拟线程落地（Java 21）：Tomcat 请求处理与 `@Async` 执行器支持虚拟线程，可配置开关；提供 jqwik 属性测试验证并发可扩展性。
8. 存储策略抽象与故障切换：以 `StorageStrategy` 抽象统一本地/对象存储能力，`StorageFailoverService` 主备切换，增强对象存储异常场景的兜底能力。
9. 对象存储高效合并：在 S3/MinIO 场景使用 Multipart Upload + `uploadPartCopy` 进行服务端分片合并，减少带宽与临时盘成本；提供残留分片定时清理任务。
10. 可观测性闭环：Actuator/Micrometer 指标 + Prometheus/Grafana；前端 Web Vitals（LCP/INP/CLS/FCP/TTFB）端到端上报，后端聚合统计并可选入库。
11. 交付与验收体系：本地 `setup/startup` 一键启动 + Docker 全栈部署 + 健康检查脚本；提供 API 冒烟与验证套件，前端 E2E 自动化回归。

## STAR 条目（可直接粘贴简历，建议 6-8 条）

### STAR 1：分片上传与断点续传
- S：大文件上传在弱网下易失败且难以恢复，重复提交导致资源浪费。
- T：实现分片上传、断点续传与上传进度可观测，并控制并发与服务端资源占用。
- A：前端分片切片+并发窗口+重试退避；Worker 计算 MD5 避免阻塞；后端分片参数强校验、限流、临时分片写入、Redis 记录进度与临时大小，最终 NIO 合并并落库。
- R：形成端到端可恢复上传链路，并具备进度查询与已上传分片枚举能力（线上指标未在仓库中沉淀，可接 Prometheus/Grafana 进一步量化）。
- 证据索引：`frontend/src/views/main/Uploader.vue`、`frontend/src/workers/md5.worker.js`、`backend/src/main/java/com/easypan/controller/FileInfoController.java`、`backend/src/main/java/com/easypan/service/impl/FileInfoServiceImpl.java`、`backend/src/main/java/com/easypan/service/impl/UploadProgressServiceImpl.java`

### STAR 2：对象存储分片合并优化（Multipart Copy）
- S：对象存储合并分片若走“下载到本地再上传”会带来带宽与临时盘成本。
- T：在 MinIO/S3 上实现更高效的分片合并，并避免阻塞请求线程。
- A：使用 Multipart Upload + `uploadPartCopy` 服务端合并；合并任务提交至虚拟线程执行器异步执行；增加定时任务清理残留分片。
- R：减少数据搬运与阻塞风险，提升合并流程的可维护性与可恢复性（缺失线上指标，建议补充合并耗时/带宽监控）。
- 证据索引：`backend/src/main/java/com/easypan/service/ChunkUploadService.java`、`backend/src/main/java/com/easypan/component/S3Component.java`、`backend/src/main/java/com/easypan/task/ChunkCleanupTask.java`、`backend/src/main/java/com/easypan/config/VirtualThreadConfig.java`

### STAR 3：统一边界治理（鉴权/参数校验/限流/性能）
- S：接口增多后，重复的登录校验/参数校验/限流逻辑分散在 Controller 中，一致性差且易漏。
- T：抽象统一的边界治理能力，做到“可复用、可审计、可演进”。
- A：AOP 实现 `@GlobalInterceptor + @VerifyParam` 登录与参数校验；Redis 计数实现 `@RateLimit`；补充 Controller 性能监控与 Mapper 慢查询监控切面。
- R：减少样板代码并提升接口一致性，问题定位更可控（可用限流触发率/慢接口数等指标进一步量化）。
- 证据索引：`backend/src/main/java/com/easypan/aspect/GlobalOperationAspect.java`、`backend/src/main/java/com/easypan/aspect/RateLimitAspect.java`、`backend/src/main/java/com/easypan/aspect/PerformanceMonitorAspect.java`、`backend/src/main/java/com/easypan/aspect/SlowQueryMonitorAspect.java`

### STAR 4：端到端可观测性（Web Vitals + Prometheus）
- S：仅看服务端耗时无法解释真实用户体验问题，需要把 RUM 与服务端指标打通。
- T：建设从前端采集到后端聚合再到监控系统展示的闭环。
- A：前端用 `web-vitals` 采集并 `sendBeacon` 上报；后端接收并提供统计接口，业务指标用 Micrometer 注册并接 Prometheus；运维侧提供 Prometheus/Grafana compose 与配置。
- R：具备 RUM+服务端指标并存的观测基础设施，便于体验与性能问题定位（看板/阈值可按业务再校准）。
- 证据索引：`frontend/src/utils/webVitals.ts`、`backend/src/main/java/com/easypan/controller/AnalyticsController.java`、`backend/src/main/java/com/easypan/metrics/CustomMetrics.java`、`ops/monitoring/prometheus.yml`

### STAR 5：登录态与 Token 安全（Refresh Token + 黑名单 + 刷新队列）
- S：Access Token 过期会导致并发请求集中失败；登出后若 Token 仍可用会带来安全风险。
- T：实现“可刷新、可失效、可抗并发”的认证闭环，并确保前后端行为一致。
- A：后端登录签发 `token/refreshToken` 并将 Refresh Token 持久化到 Redis；刷新接口校验 JWT + Redis 绑定关系；登出时删除 Refresh Token，并将 Access Token 按剩余 TTL 加入 Redis 黑名单，认证过滤器拒绝黑名单 Token；前端在请求层实现 `isRefreshing + subscribers` 队列化刷新与失败兜底，避免并发刷新风暴与重复跳转。
- R：降低并发刷新导致的雪崩风险，登出后 Token 可即时失效，认证链路可运维可审计（缺失线上指标，可补充刷新失败率/黑名单命中率监控）。
- 证据索引：`backend/src/main/java/com/easypan/controller/AccountController.java`、`backend/src/main/java/com/easypan/component/JwtTokenProvider.java`、`backend/src/main/java/com/easypan/component/JwtAuthenticationFilter.java`、`backend/src/main/java/com/easypan/component/RedisComponent.java`、`backend/src/main/java/com/easypan/service/impl/JwtBlacklistServiceImpl.java`、`frontend/src/utils/Request.ts`

### STAR 6：多租户上下文注入与配额治理（SaaS Ready）
- S：同一套系统需要服务多个租户，既要隔离数据，也要对每个租户的存储与用户规模做约束。
- T：在不重写业务代码的前提下落地多租户上下文，并把配额校验嵌入关键链路（注册/上传）。
- A：Web 拦截器从请求头 `X-Tenant-Id` 或 `*.easypan.com` 子域解析租户并写入 ThreadLocal，上下文在请求结束后清理；MyBatis-Flex 通过 `TenantFactory` 提供租户过滤的统一入口；Flyway 迁移新增 `tenant_info` 表与各核心表 `tenant_id` 字段/索引/外键；在 `TenantQuotaService` 中根据租户配置做存储配额与用户配额校验，并在上传/注册等流程调用。
- R：具备多租户扩展的工程基础，租户资源边界可控（缺失实际租户规模与配额命中率指标，可后续补齐监控）。
- 证据索引：`backend/src/main/java/com/easypan/component/TenantInterceptor.java`、`backend/src/main/java/com/easypan/component/TenantContextHolder.java`、`backend/src/main/java/com/easypan/config/TenantConfig.java`、`backend/src/main/java/com/easypan/service/TenantQuotaService.java`、`backend/src/main/resources/db/migration/V5__Add_Tenant_Support.sql`

### STAR 7：分享访问审计（异步落库 + 指标）
- S：分享链接面向访客开放，缺少可追溯审计会导致安全事件与用户纠纷难以定位。
- T：对“校验提取码/访问文件”等关键动作做结构化审计，同时不显著拖慢核心请求链路。
- A：通过 Flyway 增加 `share_access_log` 表及索引；Mapper 使用参数化 SQL 插入审计记录；Service 采用 `@Async` 异步落库并对 UA/错误信息做截断防止字段超长；WebShareController 在成功与失败路径分别记录审计，并同步累加业务指标。
- R：形成分享访问可追溯链路，支持后续风控/告警（例如失败校验突增、异常下载行为）（缺失告警阈值与看板落地，可作为下一步）。
- 证据索引：`backend/src/main/java/com/easypan/controller/WebShareController.java`、`backend/src/main/java/com/easypan/service/ShareAccessLogService.java`、`backend/src/main/java/com/easypan/service/impl/ShareAccessLogServiceImpl.java`、`backend/src/main/java/com/easypan/mappers/ShareAccessLogMapper.java`、`backend/src/main/resources/db/migration/V8__Add_Share_Access_Log.sql`、`backend/src/main/java/com/easypan/metrics/CustomMetrics.java`

### STAR 8：列表查询性能优化（游标分页 + 索引迁移）
- S：文件/分享列表可能规模很大，传统 offset 分页在大页码下性能不稳定，且易出现翻页漂移。
- T：提供稳定且高性能的分页能力，并用数据库迁移把“常用查询”优化为可复现的工程资产。
- A：新增 `/file/loadDataListCursor` 返回 `CursorPage`，以 `{createTimeMillis}_{id}` 作为游标，服务端按 `create_time desc, file_id desc` 排序并 `fetchSize=pageSize+1` 计算 `nextCursor`，同时限制 `pageSize<=100`；Mapper 使用 `(create_time, file_id) < (cursorTime, cursorId)` 的组合条件下推过滤；通过 Flyway 迁移补齐回收站递归恢复与文件名检索等关键索引。
- R：在数据量增大时仍能保持分页链路可用且顺序稳定（缺失压测/慢查询数据，可后续通过 Pg 指标与慢查询采样补齐）。
- 证据索引：`backend/src/main/java/com/easypan/controller/FileInfoController.java`、`backend/src/main/java/com/easypan/service/FileInfoService.java`、`backend/src/main/java/com/easypan/service/impl/FileInfoServiceImpl.java`、`backend/src/main/java/com/easypan/mappers/FileInfoMapper.java`、`backend/src/main/resources/db/migration/V12__Add_Folder_And_Recycle_Indexes.sql`、`backend/src/main/resources/db/migration/V13__Add_User_File_Name_Index.sql`

## 如何验证（可复现命令）
```powershell
# 后端（注意：当前仓库扫描记录中，单测存在失败用例，见下方“验证结果备注”）
mvn -B -f backend/pom.xml test

# 前端
npm -C frontend run type-check
npm -C frontend run lint:check
npm -C frontend run test:e2e
```

## 验证结果备注（基于扫描时执行记录）
- 前端：`type-check/lint:check/test:e2e` 均为 exit code 0（证据：`agent_runs/20260215-1800_full_scan/frontend_*_exit_code.txt`）
- 后端：`mvn test` exit code 1，主要因 `FileInfoServiceTest` 中 `MultiLevelCacheService` 未注入导致 NPE（证据：`agent_runs/20260215-1800_full_scan/backend_mvn_test_tail120.txt`）
