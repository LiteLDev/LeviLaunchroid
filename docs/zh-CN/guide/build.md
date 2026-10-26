# 从源码构建

本页面向想构建 LeviLauncher for Android 本体的贡献者。原生模组的构建说明位于 [原生模组快速开始](/zh-CN/guide/developer)。

## 前置要求

- Git。
- Android Studio。
- JDK 21 或更高版本。
- API 28 或更高版本的 Android SDK。
- 构建原生代码时，需要通过 Android Studio 安装 Android NDK 和 CMake 组件。

## 打开项目

```bash
git clone https://github.com/LiteLDev/LeviLaunchroid.git
cd LeviLaunchroid
```

用 Android Studio 打开项目目录，并等待 Gradle sync 完成。Gradle 设置里的项目名是 `levilauncher`，仓库名仍然是 `LeviLaunchroid`。

## 构建并运行

1. 连接 Android 设备，或启动符合支持基线的模拟器。
2. 选择 `app` 运行配置。
3. 在 Android Studio 中构建并运行。
4. 测试启动器流程前，先在测试设备上安装官方 Minecraft。

## 命令行构建

```bash
./gradlew assembleDebug
```

Windows：

```powershell
.\gradlew.bat assembleDebug
```

## 贡献者检查

提交启动器改动前，请构建应用并在设备上测试受影响流程。纯文档改动至少应通过文档构建：在 `docs/` 下运行 `npm ci`，再运行 `npm run build`（站点使用 levimc-kit 构建）。
