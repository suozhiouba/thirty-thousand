# 三万 · 时间账户 / Thirty Thousand · Time Account

> 把时间，当作唯一的本金。  
> **Treat time as your only capital.**

「人生短短三万天」——**三万**是一款 Android 本地时间记录应用。它用 30,000 天作为统一刻度，把从生日开始计算的剩余时间转化为持续流动的账户余额，再用每天一条留言，记录时间究竟花在了哪里。

**Thirty Thousand** is a local-first Android app inspired by the idea that a human life is roughly thirty thousand days. It turns the remaining time from your chosen birthday into a continuously decreasing balance, then gives you one daily note to record where your time went.

三万天是一种帮助人感知时间的方式，不是寿命预测。数字会流逝，但今天仍然可以被认真度过。  
The number is a reflection tool, not a prediction of how long anyone will live. Time moves on, but today can still be lived deliberately.

## 中文介绍

### 为什么做它

人很容易产生“以后还有很多时间”的错觉。时间账户把这件事变成一个持续可见的界面：余额每秒减少，今天结束后，一条留言留下它的去向。

它不试图制造焦虑，也不把生活变成打卡竞赛。它只提供一个安静的提醒：时间正在发生，而你正在使用它。

### 功能

- **时间账户**：输入出生日期，以生日当天零点作为起点，查看剩余天数、小时、分钟、秒和毫秒。
- **时间币余额**：1「时」等于 1 个标准日（24 小时），初始本金为 30,000 时。
- **人生刻度**：300 个格子呈现三万天进度，每格代表 100 天。
- **每日一条留言**：从工作、学习、陪伴、生活、休息或探索中选择主要投入方向，写下当天的时间去向。
- **留言封存**：每天只能成功保存一条；保存后不可修改或补写，避免历史记录被随意重构。
- **草稿自动保存**：未封存的文字只保存在本机，可以跨页面继续编辑。
- **历史账本**：按日期倒序查看留言，支持搜索日期、内容和投入方向。
- **联网校时**：至少两个公开 HTTPS 时间来源交叉核对；断网时继续运行，并明确显示当前时间状态。
- **本地备份**：导出和恢复 JSON 备份，恢复时已有日期的记录不会被覆盖。

### 隐私设计

- 不需要账号、登录、云端数据库或业务服务器。
- 生日、留言和草稿默认只保存在手机本地。
- 联网仅用于读取公开 HTTPS 响应的时间信息，不上传生日和留言。
- 校时请求仍会像普通网络请求一样暴露设备的网络信息，例如 IP 地址。
- 备份文件包含生日与留言，是明文 JSON，请自行妥善保管。
- 卸载应用会清除本地数据，请在换机或卸载前导出备份。

### 时间规则

- 总预算固定为 `30,000 × 24 小时`，余额最低为零。
- 余额达到零后仍然可以继续记录留言；三万天只是统一刻度。
- 账户时区在首次创建账户时固定，旅行时不会改变历史日记的日界线。
- 首次保存留言后，出生日期会锁定，以保证历史余额不会被重新解释。
- 毫秒是界面显示刻度，不代表网络校时可以达到毫秒精度；校时可能存在秒级误差。
- 离线时使用本机时间或上次校准后的时间估计。任何完全本地的应用都无法彻底阻止用户修改系统时间或本地数据。

### 下载与安装

打开本仓库的 **Releases / 版本发布** 页面，下载 `thirty-thousand-1.0.0.apk`，传到 Android 手机后打开安装。

- 支持 Android 8.0（API 26）及以上。
- 首次启动时选择出生日期。
- 当前 APK 使用开发签名，适合体验和源码验证；正式商店发布时请替换为自己的发布签名。

### 从源码构建

使用 Java、Android 原生 Views、SQLite 和 `java.time`，没有第三方运行时依赖。

| 配置 | 版本 |
| --- | --- |
| minSdk | 26 |
| targetSdk | 35 |
| compileSdk | 36 |
| Android Gradle Plugin | 8.10.1 |
| 独立构建脚本使用的 Build Tools | 36.0.0 |

可以用 Android Studio 打开项目并同步 Gradle。仓库暂未包含 Gradle Wrapper。

Windows 上安装 JDK 与 Android SDK 后，也可以在 Git Bash 中使用独立脚本：

```bash
# SDK 不在默认位置时，先设置 ANDROID_SDK_ROOT
bash scripts/build.sh
```

脚本会把 APK 输出到 `deliverables/`，并在 `build/` 中生成本地开发签名。签名文件不会提交到仓库；如需后续覆盖安装，请保留同一签名。正式分发前应自行配置并安全保管发布签名。

### 代码结构

```text
app/src/main/java/com/thirtythousand/app/
├── MainActivity.java    页面、导航与交互
├── LifeMath.java        日期、余额、倒计时与日界线
├── TimeAuthority.java   网络校时与离线时间估计
├── LedgerDb.java        SQLite 留言、搜索、备份与恢复
└── LifeGrid.java        三万天可视刻度
```

### 当前版本与边界

v1.0.0 是首个体验版本。APK 已完成编译与签名验证，核心日期和倒计时逻辑已完成 23 项检查。项目欢迎通过 Issues 反馈设备型号、Android 版本、操作步骤和问题现象；请不要附上私人留言或个人备份文件。

### 未来方向

可能的后续方向包括通知栏余额组件、更多时间统计视图、主题切换，以及更完善的导入导出兼容性。任何新增功能都应继续保持本地优先、低打扰和不制造焦虑的原则。

## English Introduction

### Why this exists

It is easy to feel that there will always be more time later. Thirty Thousand makes the passage of time visible without turning life into a productivity contest: the balance decreases continuously, and one note can be sealed at the end of each day.

The app is intentionally quiet. It does not rank people, reward streaks, or pressure you to perform. It simply makes the present easier to notice.

### Features

- **Time account**: choose a date of birth and see the remaining days, hours, minutes, seconds, and milliseconds.
- **Time balance**: one `shi` represents one standard day of 24 hours; the starting balance is 30,000 shi.
- **Life grid**: 300 cells visualize the 30,000-day scale; each cell represents 100 days.
- **One note per day**: choose a primary category—work, learning, companionship, life, rest, or exploration—and record where the day went.
- **Sealed entries**: only one entry can be saved per day. Once sealed, it cannot be edited or backfilled.
- **Local drafts**: unfinished notes are saved on the device so you can continue later.
- **Time ledger**: browse entries in reverse chronological order and search by date, category, or text.
- **Network time check**: cross-checks public HTTPS time responses from multiple sources; the app remains usable offline and shows its clock status.
- **Local backup**: export and restore JSON backups. Existing dates are preserved during restore.

### Privacy

- No account, login, cloud database, business backend, or analytics SDK is required.
- Birth date, notes, and drafts stay on the device by default.
- Network access is used only to read public HTTPS response time information; birth dates and notes are not uploaded.
- A time-check request still exposes ordinary network metadata such as the device IP address.
- Backups contain your birth date and notes as plain-text JSON. Keep them private.
- Uninstalling the app removes local data. Export a backup before uninstalling or changing devices.

### Time model

- The initial budget is fixed at `30,000 × 24 hours` and never goes below zero.
- Notes remain available after the balance reaches zero; the 30,000-day scale is a shared reference point.
- The account time zone is fixed when the account is created, so travel does not rewrite the diary boundary.
- The birth date is locked after the first sealed note so historical balances remain consistent.
- Milliseconds are a display unit, not a promise of millisecond network accuracy. Network synchronization can have second-level uncertainty.
- Offline mode uses the device clock or the last calibrated estimate. A fully local app cannot completely prevent manual device-clock changes or local-data changes.

### Download and install

Open the repository's **Releases** page, download `thirty-thousand-1.0.0.apk`, transfer it to an Android phone, and open it to install.

- Android 8.0 (API 26) or newer is supported.
- Choose a birth date when launching the app for the first time.
- The current APK uses a development signing key for testing and source sharing. Use your own release key for store distribution.

### Build from source

The project uses Java, Android Views, SQLite, and `java.time`, with no third-party runtime dependencies.

| Setting | Version |
| --- | --- |
| minSdk | 26 |
| targetSdk | 35 |
| compileSdk | 36 |
| Android Gradle Plugin | 8.10.1 |
| Build Tools used by the standalone script | 36.0.0 |

Open the project in Android Studio and sync Gradle. A Gradle Wrapper is not included yet.

After installing a JDK and Android SDK, you can also build on Windows from Git Bash:

```bash
# Set ANDROID_SDK_ROOT first if the SDK is not in its default location
bash scripts/build.sh
```

The script writes the APK to `deliverables/` and creates a local development key under `build/`. Signing material is ignored by Git. Keep the same key if you need to install an update over the existing development build. Use a securely managed release key for public distribution.

### Project structure

```text
app/src/main/java/com/thirtythousand/app/
├── MainActivity.java    Screens, navigation, and interaction
├── LifeMath.java        Date math, balance, countdown, and day boundaries
├── TimeAuthority.java   Network synchronization and offline time estimate
├── LedgerDb.java        SQLite entries, search, backup, and restore
└── LifeGrid.java        Three-hundred-cell life visualization
```

### Version and scope

v1.0.0 is the first public experience release. The APK has been compiled and signature-verified, and the core date/countdown logic has passed 23 focused checks. When opening an Issue, include the device model, Android version, reproduction steps, and observed behavior. Do not attach private notes or personal backup files.

### Possible next steps

Future work may include a notification balance widget, additional time summaries, theme options, and broader backup compatibility. New features should preserve the local-first, low-pressure character of the project.

## License

Released under the [MIT License](LICENSE).
