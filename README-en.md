[Читать на русском](README.md) | [阅读中文版](README-zh.md)

# ITP — Custom client for ITD

Android application for [itd](https://итд.com/) with advanced features. Not a fork of the native mobile app, uses the web version.
### To open the tweaks menu, long-press the app icon and select "Tweaks and settings".

## Features

### Content Filters

| Feature | Description |
|---------|----------|
| **Blur profanity** | Blurs posts containing profanity. Tap to reveal |
| **Traffic saver** | Completely hides all images and videos in the feed |
| **Image download** | ⬇ button in the top right corner of each photo. Saves to `Pictures/ITP/` |
| **Hide emoji clans** | Enter clan emojis — posts from these clans are fully hidden |

### Translator

Built-in post translation via Google Translate:

- **Skip languages** — comma-separated language codes (e.g. `ru`)
- **Target language** — target language (e.g. `en`)
- **Auto-mode** — all posts are translated immediately
- **Manual mode** — translation button near ❤ 💬 🔄. Tap → translates. Tap again → original |

### Appearance & Interface

| Feature | Description |
|---------|----------|
| **App language** | Interface language selection (System, English, Russian, Chinese, Spanish) |
| **Material You** | Adapts site colors to Android 12+ Material You theme. Light/Dark mode |
| **PC version** | PC-like site layout, horizontal screen support |

### Security & System

| Feature | Description |
|---------|----------|
| **PIN code** | Requires a 4-digit PIN on cold start |
| **Analytics** | Disableable anonymous basic statistics collection |

### Misc

- **Pull-to-refresh** — swipe down at the top to refresh
- **Shortcut** — long-press the app icon for quick access to Tweaks

## 🛠 Build

### Requirements

- Android SDK (API 34)
- JDK 11+
- Gradle

### Commands

```bash
# Debug APK
./gradlew assembleDebug

# Release APK (needs keystore)
./gradlew assembleRelease
```

## 📂 Structure

```text
app/src/main/java/cat/narezany/itp/
├── MainActivity.kt       # WebView + all JS injections
├── TweaksActivity.kt     # Tweaks settings
├── PinActivity.kt        # PIN entry/setup screen
```

## 📝 License

MIT
