[Читать на русском](README.md) | [Read in English](README-en.md)

# ITP — ITD 的自定义客户端

带有高级功能的 [itd](https://итд.com/) Android 应用程序。不是原生移动应用的独立分支，而是使用网页版。
### 要打开调整（Tweaks）菜单，请长按应用图标并选择“Tweaks and settings”。

## 功能

### 内容过滤

| 功能 | 描述 |
|---------|----------|
| **屏蔽脏话** | 模糊包含脏话的帖子文本。点击帖子：取消模糊 |
| **节省流量** | 完全隐藏信息流中的所有图片和视频 |
| **下载图片** | 每个帖子图片右上角的 ⬇ 按钮。保存至 `Pictures/ITP/` |
| **隐藏表情符号部落** | 输入部落的表情符号——这些部落的帖子将完全隐藏 |

### 翻译器

通过 Google 翻译内置的帖子翻译：

- **不翻译的语言** — 逗号分隔的语言代码（例如 `ru, uk`）
- **翻译为目标语言** — 目标语言（例如 `zh`）
- **自动模式** — 立即翻译所有帖子
- **手动模式** — 在 ❤ 💬 🔄 按钮旁边出现翻译按钮。点击 → 翻译 → 文本替换。再次点击 → 恢复原文

### 外观和界面

| 功能 | 描述 |
|---------|----------|
| **应用语言** | 界面语言选择（系统、英语、乌克兰语、俄语、中文、西班牙语） |
| **Material You** | 根据系统的 Material You 颜色（Android 12+）调整网站配色。支持明/暗主题 |
| **电脑版视图** | 电脑版网站布局，支持横屏 |

### 安全与系统

| 功能 | 描述 |
|---------|----------|
| **PIN 码** | 每次冷启动应用时需要 4 位 PIN 码 |
| **通知** | 后台检查网站的新事件 |
| **数据收集** | 可禁用的匿名基础数据收集 |

### 其他

- **下拉刷新** — 在页面顶部下拉即可刷新
- **快捷方式** — 长按图标快速访问 Tweaks 设置

## 🛠 构建

### 要求

- Android SDK (API 34)
- JDK 11+
- Gradle

### 指令

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (需要 keystore)
./gradlew assembleRelease
```

## 📂 结构

```text
app/src/main/java/cat/narezany/itp/
├── MainActivity.kt       # WebView + 所有 JS 注入
├── TweaksActivity.kt     # 调整设置
├── PinActivity.kt        # PIN 码输入/设置屏幕
├── NotificationService.kt# 后台通知工作服务
```

## 📝 许可证

MIT
