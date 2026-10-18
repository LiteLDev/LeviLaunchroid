---
layout: home

hero:
  name: LeviLauncher
  text: A lightweight Android launcher for Minecraft Bedrock
  tagline: Import and launch your official Minecraft installation, manage isolated versions, switch accounts, organize worlds and packs, and extend the game with native modules.
  image:
    src: /appicon.png
    alt: LeviLauncher
  actions:
    - theme: brand
      text: Get started
      link: /guide/getting-started
    - theme: alt
      text: View features
      link: /guide/features
    - theme: alt
      text: Developer docs
      link: /guide/developer
    - theme: alt
      text: Download
      link: https://github.com/LiteLDev/LeviLaunchroid/releases/latest

features:
  - title: Official game first
    details: LeviLauncher is built for legitimate Minecraft Bedrock players and requires the official Google Play version of Minecraft.
    icon: grass
  - title: Version isolation
    details: Keep multiple game versions and their data separated so testing one setup does not disturb another.
    icon: chest
  - title: Content management
    details: Import, export, back up, and organize worlds, resource packs, and launcher data from one place.
    icon: map
  - title: Account switching
    details: Manage multiple Xbox accounts in the launcher and switch between them before launching.
    icon: emerald
  - title: Quick Launch
    details: Use URI actions to open specific Minecraft screens, connect to servers, or load a world faster.
    icon: compass
  - title: Native mod support
    details: Developers can use Preloader APIs to load native SO modules, receive input callbacks, install hooks, and apply patches.
    icon: pickaxe
---

::: tip Looking for Chinese documentation?
Use the language selector or open [简体中文](/zh-CN/).
:::

## What this site covers

LeviLauncher is an open-source Android launcher for Minecraft Bedrock Edition. It focuses on practical launcher workflows first: installing the launcher, preparing an official Minecraft copy, managing versions, starting the game, and keeping player content organized.

Developer-facing Preloader APIs are still documented here, but they are part of the Developer section instead of the main entry point.

## Recommended reading order

1. [Getting Started](/guide/getting-started)
2. [Features](/guide/features)
3. [Compatibility](/guide/compatibility)
4. [Native Mod Quick Start](/guide/developer)
5. [Preloader API Reference](/api/mod)
