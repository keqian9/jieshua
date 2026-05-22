# 戒刷 (JieShua)

打开抖音/快手/B站/番茄小说等25款App时自动返回桌面，微信视频号和直播也能拦截，不误伤朋友圈和聊天。

## 拦截清单

**短视频 (12款)**：抖音、抖音极速版、抖音火山版、快手、快手极速版、微视、小红书、B站、西瓜视频、皮皮虾、美拍、YouTube

**网文小说 (13款)**：番茄小说、掌阅、掌阅免费版、起点读书、QQ阅读、微信读书、七猫免费小说、书旗小说、飞卢小说、晋江文学城、追书神器、宜搜小说

**微信内部**：视频号、直播（仅检测UI导航标签，不扫描正文，不会误伤朋友圈和聊天）

## 下载

👉 **[下载 APK (v1.0.0)](https://github.com/keqian9/jieshua/releases/latest/download/app-debug.apk)**

或者用手机扫描下方二维码直接下载：

![下载二维码](qrcode.png)

## 使用方法

1. 下载 APK 安装（手机提示「未知来源」时点允许）
2. 打开 App，点击「去开启」
3. 在无障碍设置中找到「戒刷」，打开开关
4. 完成。打开任何拦截列表里的App都会自动退回桌面

## 原理

双层拦截：
- **包名拦截**：检测前台App包名，命中黑名单直接返回桌面
- **内容拦截**：仅在微信内检测 UI 导航标签(contentDescription)，识别视频号/直播入口后自动返回

使用 Android AccessibilityService，无网络权限，不收集任何数据。

## 自定义

编辑 `app/src/main/java/io/github/chayanforyou/fguard/services/FGuardService.kt`：

- `monitoredApps` — 按包名拦截的App列表
- `blockedContentDescriptions` — 按界面标签拦截的关键词（仅微信内生效）

## License

MIT. Forked from [fGuard](https://github.com/chayanforyou/fGuard).
