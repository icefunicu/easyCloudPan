# Ops 目录说明

`ops/` 用于统一存放部署与启动相关脚本，并按职责分层。

## 目录结构

- `ops/local/`：本地开发启动脚本
- `ops/docker/`：Docker 部署脚本与 Compose 配置
- `ops/lib/`：脚本公共函数
- `ops/tools/`：辅助工具脚本

## 入口脚本

- 本地初始化：`ops/local/setup.bat`
- 本地启动：`ops/local/startup.bat`
- Docker 部署：`ops/docker/deploy_docker.bat`
- Docker 停止：`ops/docker/stop_docker.bat`
