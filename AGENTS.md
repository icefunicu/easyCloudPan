# EasyCloudPan Agent 协作规范 (AGENTS.md)

## 1. 技术栈底座
- **JDK**: 21+ (必须支持 Virtual Threads)
- **Node.js**: 18+
- **DB**: PostgreSQL 15+
- **Migration**: Flyway (严禁手动修改本地 DB 结构而不同步脚本)

## 2. 编码规约 (Lombok 强制)
为了保持代码整洁，所有实体类遵循以下规范：
- **PO/DTO/VO**: 必须使用 Lombok `@Data` 或 `@Getter/@Setter` 注解，移除手动生成的样板代码。
- **字段命名**: 布尔类型字段避免以 `is` 开头（若必须，请手动配置属性名以确保兼容性，如 `private Boolean admin` 处理 `isAdmin` 逻辑）。
- **构造函数**: 优先使用 `@NoArgsConstructor` 和 `@AllArgsConstructor`。

## 3. 数据库变更规范
- 每一次数据库结构的修改，必须在 `backend/src/main/resources/db/migration` 下创建新的版本脚本（格式：`V[时间戳/版本]__描述.sql`）。

## 4. Agent 协作流程 (System Spec)
- **计划先行**: Agent 在处理大型任务（涉及多文件修改）前，必须产出 `implementation_plan.md`。
- **用户确认**: 只有在用户确认后方可执行。
- **收尾清理**: 任务结束后，Agent 负责更新 `walkthrough.md`。

## 5. 项目结构认知
- `ops/local/setup.bat` 是环境一致性的唯一入口。
- `ops/docker/docker-compose.yml` 包含 Redis, MinIO, PostgreSQL 的标准联调环境。
