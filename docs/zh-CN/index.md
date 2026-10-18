---
layout: home

hero:
  name: LeviLauncher
  text: 面向 Minecraft 基岩版的轻量 Android 启动器
  tagline: 启动官方 Minecraft，管理隔离版本、账号、世界、资源包与备份，并通过 native 模块扩展游戏能力。
  image:
    src: /appicon.png
    alt: LeviLauncher
  actions:
    - theme: brand
      text: 快速开始
      link: /zh-CN/guide/getting-started
    - theme: alt
      text: 功能概览
      link: /zh-CN/guide/features
    - theme: alt
      text: 开发者文档
      link: /zh-CN/guide/developer
    - theme: alt
      text: 下载
      link: https://github.com/LiteLDev/LeviLaunchroid/releases/latest

features:
  - title: 官方游戏优先
    details: LeviLauncher 面向合法 Minecraft 基岩版玩家，要求设备上存在来自 Google Play 的官方 Minecraft。
    icon: grass
  - title: 多版本隔离
    details: 独立管理多个 Minecraft 版本和数据目录，测试、游玩、回退互不干扰。
    icon: chest
  - title: 内容管理
    details: 在启动器内导入、导出、备份和整理世界、资源包与启动器数据。
    icon: map
  - title: 账号切换
    details: 在启动器内管理多个 Xbox 账号，并在启动前切换到需要使用的账号。
    icon: emerald
  - title: 快速启动
    details: 使用 URI 操作直接打开指定游戏页面、连接服务器或加载本地世界。
    icon: compass
  - title: Native Mod 支持
    details: 开发者可以通过 Preloader API 加载 native SO 模块、接收输入回调、安装 hook 并应用 patch。
    icon: pickaxe
---

::: tip Need English documentation?
Use the language selector or open [English](/).
:::

## 本站覆盖内容

LeviLauncher 是一个开源 Android Minecraft 基岩版启动器。本文档优先讲清楚启动器本身：如何安装、准备官方 Minecraft、管理版本、启动游戏、整理玩家内容，以及如何处理兼容性问题。

Preloader API 仍然保留在本站，但它属于“开发者”分类，不再是文档的主入口。

## 推荐阅读顺序

1. [快速开始](/zh-CN/guide/getting-started)
2. [功能概览](/zh-CN/guide/features)
3. [兼容性与常见问题](/zh-CN/guide/compatibility)
4. [Native Mod 快速开始](/zh-CN/guide/developer)
5. [Preloader API 参考](/zh-CN/api/mod)
