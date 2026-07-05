# 兼容性与常见问题

## 支持设备

LeviLauncher 面向 Android 9.0 或更高版本的 ARM64 设备。可用内存和存储越多，管理
多个版本、大型世界或资源包时体验越好。

## Minecraft 要求

LeviLauncher 需要 Google Play 安装的正版 Minecraft Bedrock Edition。它面向合法
玩家，不提供 Minecraft 授权。

如果没有安装 Minecraft、安装来源不受支持，或版本过旧，启动器可能会在启动游戏前停止。

## Minecraft 版本

应用当前会拒绝低于 1.21.80 的 Minecraft 版本。导入版本如果需要独立数据，请启用版本隔离。

## 存储与迁移

启动器可能需要先迁移旧数据目录。迁移成功并确认世界、资源包、设置都可用之前，不要删除旧目录。

备份会保存在启动器管理的备份位置。重要世界建议额外保留一份应用存储之外的副本。

## Native 模块

Native 模块会在游戏进程中运行原生代码。只安装可信来源的模块，并优先在隔离版本中测试。

## 常见问题

### 为什么启动器提示找不到 Minecraft？

请从 Google Play 安装或更新官方 Minecraft，然后重新打开 LeviLauncher。

### 为什么导入版本启动失败？

导入版本通常需要版本隔离。请在实例设置中启用后重试。

### 开发者 API 问题去哪里看？

请阅读 [开发者](/zh-CN/guide/developer) 分类。Preloader API 参考刻意放在该分类下，
而不是普通用户指南里。
