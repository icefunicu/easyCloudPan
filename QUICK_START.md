# 🚀 EasyCloudPan 快速启动指南

本指南专为新手设计，帮助您在 Windows 环境下**零门槛**启动项目。

---

## 🛠️ 第一步：环境准备

本脚本已实现了**自动化配置**，您只需安装最基础的运行环境：

1.  **JDK 1.8+** (Java 运行环境) & **Maven 3.6+** (构建工具)
    -   [下载安装 JDK](https://adoptium.net/temurin/releases/?version=8)
    -   [下载安装 Maven](https://maven.apache.org/download.cgi)
    -   *安装后请确保配置了 `JAVA_HOME` 和 `Path` 环境变量。*

2.  **Node.js 16+** (前端运行环境)
    -   [下载安装](https://nodejs.org/en/download/)
    -   *建议安装 LTS 版本。*

3.  **MySQL 8.0+** (数据库)
    -   [下载安装](https://dev.mysql.com/downloads/installer/)
    -   *安装时请记住设置的 `root` 用户密码。*
    -   *由于数据库安装涉及系统服务，脚本无法自动完成，请手动安装。*

> **关于 Redis 和 FFmpeg**:
> `setup.bat` 脚本会自动检测并在 `tools/` 目录下下载便携版 Redis 和 FFmpeg，无需您手动安装配置！

---

## ⚙️ 第二步：初始化配置

1.  在项目根目录下找到 `setup.bat` 文件。
2.  双击运行 `setup.bat`。
3.  **自动工具下载**: 脚本会首先检查并下载 Redis 和 FFmpeg。如果下载失败，请根据提示手动下载。
4.  按照提示输入配置信息：
    -   **MySQL 密码**: 您安装数据库时设置的密码（默认为 `1234`）。
    -   **Redis 地址/端口**: 直接回车使用默认值即可（脚本会自动启动本地 Redis）。
    -   **文件存储路径**: 输入您想存放网盘文件的目录（例如 `D:/EasyPanData`）。

> 脚本会自动检测环境、生成配置文件并安装前端依赖，请耐心等待直到提示“配置完成”。

---

## ▶️ 第三步：一键启动

1.  在项目根目录下找到 `startup.bat` 文件。
2.  双击运行 `startup.bat`。
3.  脚本会自动启动后端和前端服务。
4.  稍等片刻，浏览器会自动打开访问地址：`http://localhost:5173`。

---

## ❓ 常见问题

**Q: 双击 bat 文件闪退怎么办？**
A: 请在文件夹空白处按住 `Shift` + 右键，选择“在此处打开 Powershell 窗口”，然后输入 `./setup.bat` 运行，查看具体报错信息。通常是环境变量未配置好。

**Q: 提示 `npm install` 失败？**
A: 可能是网络问题。请尝试设置淘宝镜像源：
```bash
npm config set registry https://registry.npmmirror.com
```
然后重新运行 `setup.bat`。

**Q: 启动后页面报错或无法登录？**
A: 请检查后端控制台窗口（不要关闭它）是否有报错日志。常见原因是数据库连接失败或 Redis 未启动。

---

*祝您使用愉快！*
