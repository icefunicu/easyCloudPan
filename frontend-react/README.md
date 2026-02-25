# frontend-react

`frontend-react` 是基于现有 EasyCloudPan 后端接口重写的新前端实现，和旧 `frontend/` 完全隔离，不会影响旧前端代码。

## 技术栈

- React 19 + TypeScript 5
- Vite 7
- Ant Design 5
- TanStack Query 5
- Zustand 5
- Framer Motion

## 功能覆盖

- 账号：登录、注册、重置密码、QQ 回调、OAuth 回调与补注册
- 文件：分类浏览、目录导航、搜索、新建目录、重命名、移动、删除、下载、分享
- 上传：MD5 计算、分片并发上传、断点续传、暂停/恢复、转码状态监听
- 分享：我的分享、提取码校验、访客浏览、保存到我的网盘、取消分享
- 回收站：恢复和彻底删除
- 管理端：用户管理、空间管理、文件管理、系统设置
- 预览：图片/视频/音频/PDF/TXT/CODE/Excel

## 运行

```powershell
cd frontend-react
npm install
npm run dev
```

默认地址：`http://localhost:5176`

## 构建与校验

```powershell
npm run type-check
npm run build
```

期望结果：

- `type-check` 无 TypeScript 错误
- `build` 成功输出 `dist/`

## 环境变量（可选）

- `VITE_SIGNATURE_SECRET`：请求签名密钥（默认 `easypan-default-secret`）
