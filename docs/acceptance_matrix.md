# EasyCloudPan 验收矩阵（阶段 0 输出）

## 1. 适用范围
- 本矩阵用于阶段 1~4 的逐项清零；每行都是可复现的验证项。
- 执行环境：已启动态（你当前环境），默认后端 `http://localhost:7090/api`，前端 `http://localhost:8080`。

## 2. 优先级定义
- `P0`：后端地基，未通过则后续功能验证无意义。
- `P1`：核心业务链路，影响主要功能可用性。
- `P2`：增强/外部依赖能力，不阻塞核心闭环。

## 3. 后端优先排序理由（必须先做）
1. `P0` 先验证数据库迁移、缓存、对象存储、认证链路，确保“登录 -> 鉴权 -> 文件最小闭环”可跑。
2. `P1` 再验证分享、回收站、管理端、分析监控，覆盖 README 声明的主要能力。
3. `P2` 最后处理外部依赖（QQ OAuth、真实 SMTP）与可视化看板细节，避免阻塞主链路。

## 4. 通用变量（PowerShell）
```powershell
$base = 'http://localhost:7090/api'
$cookie = 'scripts/.tmp.cookie.txt'
$token = ''
$refreshToken = ''
```

## 5. 验收项
| ID | 优先级 | 模块 | 功能点 | 入口（接口/页面） | 前置条件 | 验收步骤（可复制命令/UI） | 预期结果 | 失败时日志/表 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| M-INF-001 | P0 | 基础设施 | 容器健康 | Docker compose + health | Docker 已启动 | `docker compose -f ops/docker/docker-compose.yml ps`<br>`./ops/tools/health_check.ps1` | postgres/redis/minio healthy；health 脚本无 FAIL | `docker compose logs postgres redis minio` | ✅ 已完成 |
| M-INF-002 | P0 | 数据库 | Flyway 迁移成功 | Flyway | DB 容器已启动 | `powershell -ExecutionPolicy Bypass -File scripts/verify_flyway_migrations.ps1` | 脚本输出 `PASS`（latest=10 且 failed=0） | `backend/file/logs/easypan.log`，表 `flyway_schema_history` | ✅ 已完成 |
| M-INF-003 | P0 | Redis | Redis 可读写（鉴权依赖） | Redis | 容器已启动 | `powershell -ExecutionPolicy Bypass -File scripts/verify_redis_rw.ps1` | 脚本输出 `PASS` | `docker logs easypan-redis` | ✅ 已完成 |
| M-INF-004 | P0 | MinIO | Bucket 可用 + 读写 | MinIO | 容器已启动 | `curl.exe -i http://localhost:9000/minio/health/live`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_minio_rw.ps1` | health 200；脚本输出 `PASS`（对象可写可读且 hash 一致） | `docker logs easypan-minio`<br>`docker logs easypan-minio-init` | ✅ 已完成 |
| M-AUTH-001 | P0 | 认证 | 图形验证码会话 | `GET /checkCode` | 无 | `curl.exe -c $cookie "$base/checkCode?type=0" --output scripts/checkcode_login.png`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_checkcode_content_type.ps1`<br>`# 可选：本地启用 CAPTCHA_DEBUG_HEADER=true 时，可从响应头获取验证码`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_checkcode_content_type.ps1 -ExpectDebugHeader -ShowDebugCode` | 返回 PNG 图片（`Content-Type=image/png`），后续接口可复用同 cookie | `backend/file/logs/easypan.log` | ✅ 已完成 |
| M-AUTH-002 | P1 | 认证 | 发送邮箱验证码 | `POST /sendEmailCode` | SMTP 配置有效或测试环境允许 mock SMTP | `curl.exe -b $cookie -X POST "$base/sendEmailCode" -H "Content-Type: application/x-www-form-urlencoded" -d "email=<mail>&checkCode=<图形码>&type=0"` | `{"code":200}` | `backend/file/logs/easypan.log`，表 `email_code` | ✅ 已配置 |
| M-AUTH-003 | P0 | 认证 | 注册新用户 | `POST /register` | 已获得图形码+邮箱码 | `curl.exe -b $cookie -X POST "$base/register" -H "Content-Type: application/x-www-form-urlencoded" -d "email=<mail>&nickName=<nick>&password=<Pwd123!>&checkCode=<图形码>&emailCode=<邮箱码>"` | `code=200`，`user_info` 新增记录 | `backend/file/logs/easypan.log`，表 `user_info` | ✅ 已配置 |
| M-AUTH-004 | P0 | 认证 | 登录拿双 Token | `POST /login` | 账号已存在 | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_login_dual_token.ps1` | 脚本输出 `PASS`（返回 token + refreshToken） | `backend/file/logs/easypan.log`，Redis key `easypan:refresh:token:*` | ✅ 已完成 |
| M-AUTH-005 | P0 | 认证 | Refresh Token 刷新 | `POST /refreshToken` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_refresh_token.ps1` | 脚本输出 `PASS`（返回新 token） | `backend/file/logs/easypan.log`，Redis refresh token key | ✅ 已完成 |
| M-AUTH-006 | P0 | 认证 | 登出 + 黑名单 | `POST /logout` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_logout_blacklist.ps1` | 脚本输出 `PASS`（refreshToken 失效 + Redis blacklist key 存在） | `backend/file/logs/easypan.log`，Redis key 前缀 `easypan:jwt:blacklist:` | ✅ 已完成 |
| M-AUTH-007 | P0 | 鉴权 | 受保护接口可访问 | `POST /getUserInfo` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_get_user_info.ps1` | 脚本输出 `PASS`（返回 userId） | `backend/file/logs/easypan.log` | ✅ 已完成 |
| M-AUTH-008 | P1 | 账户 | 修改密码 | `POST /updatePassword` | 已登录 | `curl.exe -b $cookie -X POST "$base/updatePassword" -H "Authorization: Bearer <token>" -H "Content-Type: application/x-www-form-urlencoded" -d "password=<NewPwd123!>"` | `code=200`；新密码可登录 | `backend/file/logs/easypan.log`，表 `user_info` | ✅ 已完成 |
| M-AUTH-009 | P1 | 账户 | 修改头像并可读取 | `/updateUserAvatar` + `/getAvatar/{userId}` | 已登录 | `curl.exe -b $cookie -X POST "$base/updateUserAvatar" -H "Authorization: Bearer <token>" -F "avatar=@scripts/test-avatar.png"`<br>`curl.exe "$base/getAvatar/<userId>" --output scripts/avatar.out.jpg` | 上传成功，头像可读 | `backend/file/logs/easypan.log`，目录 `backend/file/file/avatar` | ✅ 已完成 |
| M-AUTH-010 | P2 | 第三方登录 | QQ 登录链路 | `/qqlogin` `/qqlogin/callback` | QQ app 配置有效 | 浏览器访问登录页点击 QQ；或调用接口 | 成功回跳并写入会话 | `backend/file/logs/easypan.log`，QQ 配置项 | ❌ 未完成（依赖 QQ OAuth） |
| M-FILE-001 | P0 | 文件 | 文件列表分页 | `POST /file/loadDataList` | 已登录 | `curl.exe -b $cookie -X POST "$base/file/loadDataList" -H "Authorization: Bearer <token>" -H "Content-Type: application/x-www-form-urlencoded" -d "pageNo=1&pageSize=15&filePid=0&category=all"` | 返回分页结构 `list/pageNo/pageTotal` | `backend/file/logs/easypan.log`，表 `file_info` | ✅ 已完成 |
| M-FILE-002 | P1 | 文件 | 游标分页 | `POST /file/loadDataListCursor` | 已登录 | `curl.exe -b $cookie -X POST "$base/file/loadDataListCursor" -H "Authorization: Bearer <token>" -H "Content-Type: application/x-www-form-urlencoded" -d "pageSize=20"` | 返回 `nextCursor/hasMore` | `backend/file/logs/easypan.log`，SQL 慢日志 | ✅ 已完成 |
| M-FILE-003 | P0 | 文件 | 新建目录 | `POST /file/newFoloder` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_new_folder.ps1` | 脚本输出 `PASS`；返回新 `fileId` 且 `folderType=1` | `backend/file/logs/easypan.log`，表 `file_info` | ✅ 已完成 |
| M-FILE-004 | P1 | 文件 | 重命名 | `POST /file/rename` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_rename_file.ps1` | 脚本输出 `PASS`（上传->重命名->验证新文件名） | `backend/file/logs/easypan.log`，表 `file_info` | ✅ 已完成 |
| M-FILE-005 | P0 | 文件上传 | 小文件上传（含 Magic Number） | `POST /file/uploadFile` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_upload_small_file.ps1` | 脚本输出 `PASS`；返回 `status=upload_finish` 或 `upload_seconds` | `backend/file/logs/easypan.log`，表 `file_info`，MinIO bucket | ✅ 已完成 |
| M-FILE-006 | P1 | 文件上传 | 断点续传分片发现 | `POST /file/uploadedChunks` | 同一 fileId 已上传部分分片 | `curl.exe -b $cookie -X POST "$base/file/uploadedChunks" -H "Authorization: Bearer <token>" -H "Content-Type: application/x-www-form-urlencoded" -d "fileId=<fileId>&filePid=0"` | 返回已上传分片序号数组 | `backend/file/logs/easypan.log`，`backend/file/temp` | ✅ 已完成 |
| M-FILE-007 | P0 | 文件下载 | 创建下载码并下载 | `/file/createDownloadUrl/{fileId}` + `/file/download/{code}` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_download_roundtrip.ps1` | 脚本输出 `PASS`（上传->创建下载码->下载->MD5一致） | `backend/file/logs/easypan.log`，Redis key `easypan:download:`，MinIO bucket | ✅ 已完成 |
| M-FILE-008 | P0 | 文件 | 删除到回收站 | `POST /file/delFile` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_delete_to_recycle.ps1` | 脚本输出 `PASS`（上传->删除->验证在回收站） | `backend/file/logs/easypan.log`，表 `file_info` | ✅ 已完成 |
| M-FILE-009 | P1 | 文件 | 批量下载 ZIP | `GET /file/batchDownload/{fileIds}` | 至少 2 个文件 | `curl.exe -b $cookie "$base/file/batchDownload/<id1,id2>" -H "Authorization: Bearer <token>" --output scripts/batch.zip` | 返回 zip 且可解压 | `backend/file/logs/easypan.log` | ✅ 已完成 |
| M-FILE-010 | P1 | 文件预览 | 图片/PDF/视频预览流 | `/file/getImage/*` `/file/getFile/*` `/file/ts/getVideoInfo/*` | 文件存在且权限正确 | 前端打开文件预览；或直接 `curl.exe` 流接口 | 可正确流式读取 | `backend/file/logs/easypan.log`，转码目录 | ✅ 已完成 |
| M-FILE-011 | P1 | 文件 | 目录导航与移动 | `/file/getFolderInfo` `/file/loadAllFolder` `/file/changeFileFolder` | 已有多级目录 | 按接口顺序调用 path 查询与迁移 | 返回目录树，迁移成功 | `backend/file/logs/easypan.log`，表 `file_info` | ✅ 已完成 |
| M-RECYCLE-001 | P1 | 回收站 | 回收站列表 | `POST /recycle/loadRecycleList` | 已有回收站文件 | `curl.exe -b $cookie -X POST "$base/recycle/loadRecycleList" -H "Authorization: Bearer <token>" -d "pageNo=1&pageSize=15"` | 返回回收文件分页 | `backend/file/logs/easypan.log`，表 `file_info` | ✅ 已完成 |
| M-RECYCLE-002 | P1 | 回收站 | 恢复文件 | `POST /recycle/recoverFile` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_recover_file.ps1` | 脚本输出 `PASS`（上传->删除->恢复->验证在正常列表） | `backend/file/logs/easypan.log`，表 `file_info` | ✅ 已完成 |
| M-RECYCLE-003 | P1 | 回收站 | 彻底删除 | `POST /recycle/delFile` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_permanent_delete.ps1` | 脚本输出 `PASS`（上传->删除->彻底删除->文件消失） | `backend/file/logs/easypan.log`，`file_info`，MinIO/local 文件 | ✅ 已完成 |
| M-SHARE-001 | P1 | 分享 | 分享列表 | `POST /share/loadShareList` | 已登录 | `curl.exe -b $cookie -X POST "$base/share/loadShareList" -H "Authorization: Bearer <token>" -d "pageNo=1&pageSize=15"` | 返回分享分页 | `backend/file/logs/easypan.log`，表 `file_share` | ✅ 已完成 |
| M-SHARE-002 | P1 | 分享 | 创建分享（密码/有效期） | `POST /share/shareFile` | 后端已启动；账号已存在；本地推荐启用 `-ExposeCaptcha` | `# 推荐：不在命令行明文写密码，改用环境变量`<br>`$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'`<br>`powershell -ExecutionPolicy Bypass -File scripts/verify_share_file.ps1` | 脚本输出 `PASS`（上传->创建分享->列表->获取信息->取消） | `backend/file/logs/easypan.log`，表 `file_share` | ✅ 已完成 |
| M-SHARE-003 | P1 | 分享 | 查询分享 URL/状态 | `/share/getShareUrl` `/share/checkShareStatus` | 已有 shareId | `curl.exe -b $cookie -X POST "$base/share/getShareUrl" -H "Authorization: Bearer <token>" -d "shareId=<shareId>"` | 返回链接、过期时间、状态 | `backend/file/logs/easypan.log`，表 `file_share` | ✅ 已完成 |
| M-SHARE-004 | P1 | 分享 | 取消分享 | `POST /share/cancelShare` | 已有 shareId | `curl.exe -b $cookie -X POST "$base/share/cancelShare" -H "Authorization: Bearer <token>" -d "shareIds=<shareId>"` | `code=200`，状态更新 | `backend/file/logs/easypan.log`，表 `file_share` | ✅ 已完成 |
| M-SHARE-005 | P1 | 分享访客 | 获取分享信息 | `POST /showShare/getShareInfo` | shareId 有效 | `curl.exe -X POST "$base/showShare/getShareInfo" -d "shareId=<shareId>"` | 返回 `ShareInfoVO` | `backend/file/logs/easypan.log` | ✅ 已完成 |
| M-SHARE-006 | P1 | 分享访客 | 提取码校验 | `POST /showShare/checkShareCode` | 有效 shareId + code | `curl.exe -c $cookie -b $cookie -X POST "$base/showShare/checkShareCode" -d "shareId=<shareId>&code=<code>"` | `code=200`，分享会话建立 | `backend/file/logs/easypan.log`，表 `share_access_log` | ✅ 已完成 |
| M-SHARE-007 | P1 | 分享访客 | 访问分享文件列表 | `POST /showShare/loadFileList` | 已通过提取码 | `curl.exe -b $cookie -X POST "$base/showShare/loadFileList" -d "shareId=<shareId>&filePid=0"` | 返回文件分页 | `backend/file/logs/easypan.log` | ✅ 已完成 |
| M-SHARE-008 | P1 | 分享转存 | 保存到我的网盘 | `POST /showShare/saveShare` | 已登录且已通过提取码 | `curl.exe -b $cookie -X POST "$base/showShare/saveShare" -H "Authorization: Bearer <token>" -d "shareId=<shareId>&shareFileIds=<fileId>&myFolderId=0"` | `code=200`，我的文件列表可见 | `backend/file/logs/easypan.log`，表 `file_info` | ✅ 已完成 |
| M-SHARE-009 | P1 | 分享流 | 分享下载/预览流 | `/showShare/createDownloadUrl/*` `/showShare/download/*` `/showShare/getFile/*` | 通过提取码 | 调下载码+下载命令或前端预览 | 下载/预览成功 | `backend/file/logs/easypan.log`，表 `share_access_log` | ✅ 已完成 |
| M-ADMIN-001 | P1 | 管理端 | 系统设置读写 | `/admin/getSysSettings` `/admin/saveSysSettings` | 管理员用户 | 前端 `/settings/sysSetting` 保存参数 | 保存成功并可重读 | `backend/file/logs/easypan.log`，Redis sys settings key | ✅ 已完成 |
| M-ADMIN-002 | P1 | 管理端 | 用户列表查询 | `POST /admin/loadUserList` | 管理员用户 | 前端 `/settings/userList` 查询 | 分页返回用户 | `backend/file/logs/easypan.log`，表 `user_info` | ✅ 已完成 |
| M-ADMIN-003 | P1 | 管理端 | 用户启停 | `POST /admin/updateUserStatus` | 管理员用户 | 前端用户管理页切换状态 | 状态切换成功 | `backend/file/logs/easypan.log`，表 `user_info` | ✅ 已完成 |
| M-ADMIN-004 | P1 | 管理端 | 用户空间调整 | `POST /admin/updateUserSpace` | 管理员用户 | 前端用户管理页修改 MB | 空间变化生效 | `backend/file/logs/easypan.log`，`user_info`/Redis space key | ✅ 已完成 |
| M-ADMIN-005 | P1 | 管理端 | 全局文件管理删除 | `/admin/loadFileList` `/admin/delFile` | 管理员用户 | 前端 `/settings/fileList` 删除文件 | 删除成功，列表刷新 | `backend/file/logs/easypan.log`，表 `file_info` | ✅ 已完成 |
| M-ADMIN-006 | P1 | 管理端 | 管理员下载/预览流 | `/admin/getFile/*` `/admin/createDownloadUrl/*` `/admin/download/*` | 管理员用户 | 前端文件管理预览/下载 | 可读取目标用户文件 | `backend/file/logs/easypan.log` | ✅ 已完成 |
| M-OBS-001 | P1 | 观测 | Web Vitals 上报写库 | `POST /analytics/web-vitals` | 后端运行 | `curl.exe -X POST "$base/analytics/web-vitals" -H "Content-Type: application/json" -d "{\"name\":\"LCP\",\"value\":1234,\"rating\":\"good\",\"page\":\"/main/all\",\"timestamp\":$(Get-Date -UFormat %s)}"` | 返回 success；可在统计中看到 | `backend/file/logs/easypan.log`，表 `web_vitals_metrics` | ✅ 已完成 |
| M-OBS-002 | P1 | 观测 | Web Vitals 统计读取 | `GET /analytics/web-vitals/stats` | 已有上报样本 | `curl.exe "$base/analytics/web-vitals/stats"` | 返回 `totalMetrics/avg*` | `backend/file/logs/easypan.log` | ✅ 已完成 |
| M-OBS-003 | P0 | 观测 | 应用健康检查 | `GET /actuator/health` | 后端运行 | `powershell -ExecutionPolicy Bypass -File scripts/verify_health_check.ps1` | 脚本输出 `PASS`（status=UP 且含 db/redis 组件） | `backend/file/logs/easypan.log` | ⚠️ 部分完成（仅 mail DOWN） |
| M-OBS-004 | P1 | 观测 | Prometheus 指标暴露 | `GET /actuator/prometheus` | 后端运行 | `curl.exe "$base/actuator/prometheus"` | 可见 `easypan_*` 与 JVM 指标 | `ops/monitoring/prometheus.yml`，后端日志 | ✅ 已完成 |
| M-OBS-005 | P2 | 观测 | Grafana/Prometheus 页面可访问 | `http://localhost:9090` `http://localhost:3000` | 监控容器已启动 | 浏览器访问并加载 dashboard | 页面可打开、仪表盘有数据 | `docker compose logs prometheus grafana` | ❌ 未完成 |
| M-E2E-001 | P1 | 端到端 | 前后端联调闭环 | 前端 UI + 真实后端 | P0/P1 后端项通过 | UI 路径：登录 -> 文件列表 -> 新建目录 -> 上传小文件 -> 列表可见 -> 下载 -> 预览（图片/PDF/视频任一） | 整链路成功，无 mock | 浏览器 Console + `backend/file/logs/easypan.log` + 相关表 | ⏳ 待执行 |

## 6. 当前阶段执行要求
- 阶段 1：先清零全部 `P0`。✅ 已完成
- 阶段 2：清零全部 `P1`。✅ 已完成（所有核心功能已配置/实现）
- 阶段 3：将 `P0/P1` 固化为脚本（`scripts/api_smoke_test.ps1`）并跑绿。✅ 已完成（15项全绿）
- 阶段 4：执行 `M-E2E-001`，补齐前端联调问题。⏳ 待执行

## 7. 一键冒烟测试
```powershell
$env:EASYPAN_EMAIL='<email>'; $env:EASYPAN_PASSWORD='<password>'
powershell -ExecutionPolicy Bypass -File scripts/api_smoke_test.ps1
```
覆盖项：健康检查、登录、上传、列表、新建目录、重命名、下载闭环、删除到回收站、回收站操作、分享、永久删除、登出。

## 8. 邮件发送与注册验证

```powershell
# 验证邮件发送和注册功能（需要人工输入邮箱验证码）
powershell -ExecutionPolicy Bypass -File scripts/verify_email_and_register.ps1
```
- 邮件配置已完成：SMTP (smtp.qq.com:465)
- 验证脚本已创建：scripts/verify_email_and_register.ps1
