import { defineConfig, type DefaultTheme } from "vitepress";

const repo = "https://github.com/LiteLDev/LeviLaunchroid";
const releases = `${repo}/releases`;
const base = process.env.VITEPRESS_BASE ?? "/";

function buildEnglishSidebar(localePrefix: string): DefaultTheme.SidebarItem[] {
  return [
    {
      text: "Guide",
      items: [
        { text: "Getting Started", link: `${localePrefix}guide/getting-started` },
        { text: "Features", link: `${localePrefix}guide/features` },
        { text: "Compatibility", link: `${localePrefix}guide/compatibility` },
      ],
    },
    {
      text: "Developer",
      items: [
        { text: "Native Mod Quick Start", link: `${localePrefix}guide/developer` },
        { text: "Build from Source", link: `${localePrefix}guide/build` },
        { text: "Mod Entry API", link: `${localePrefix}api/mod` },
        { text: "Mod Menu API", link: `${localePrefix}api/mod-menu` },
        { text: "Config API", link: `${localePrefix}api/config` },
        { text: "Input API", link: `${localePrefix}api/input` },
        { text: "Hook API", link: `${localePrefix}api/hook` },
        { text: "Signature API", link: `${localePrefix}api/signature` },
        { text: "Patch API", link: `${localePrefix}api/patch` },
        {
          text: "Memory Hook Macros",
          link: `${localePrefix}api/memory-hook-macros`,
        },
        { text: "Types and Macros", link: `${localePrefix}api/types-and-macros` },
      ],
    },
  ];
}

function buildChineseSidebar(localePrefix: string): DefaultTheme.SidebarItem[] {
  return [
    {
      text: "指南",
      items: [
        { text: "快速开始", link: `${localePrefix}guide/getting-started` },
        { text: "功能概览", link: `${localePrefix}guide/features` },
        { text: "兼容性与常见问题", link: `${localePrefix}guide/compatibility` },
      ],
    },
    {
      text: "开发者",
      items: [
        { text: "Native Mod 快速开始", link: `${localePrefix}guide/developer` },
        { text: "从源码构建", link: `${localePrefix}guide/build` },
        { text: "Mod 入口 API", link: `${localePrefix}api/mod` },
        { text: "Mod Menu API", link: `${localePrefix}api/mod-menu` },
        { text: "Config API", link: `${localePrefix}api/config` },
        { text: "Input API", link: `${localePrefix}api/input` },
        { text: "Hook API", link: `${localePrefix}api/hook` },
        { text: "Signature API", link: `${localePrefix}api/signature` },
        { text: "Patch API", link: `${localePrefix}api/patch` },
        { text: "Memory Hook 宏", link: `${localePrefix}api/memory-hook-macros` },
        { text: "Types 与宏", link: `${localePrefix}api/types-and-macros` },
      ],
    },
  ];
}

function buildEnglishNav(): DefaultTheme.NavItem[] {
  return [
    { text: "Guide", link: "/guide/getting-started" },
    { text: "Features", link: "/guide/features" },
    {
      text: "Developer",
      items: [
        { text: "Native Mod Quick Start", link: "/guide/developer" },
        { text: "API Reference", link: "/api/mod" },
        { text: "Build from Source", link: "/guide/build" },
      ],
    },
    { text: "Privacy Policy", link: "/privacy-policy" },
    { text: "Downloads", link: releases },
    { text: "GitHub", link: repo },
  ];
}

function buildChineseNav(): DefaultTheme.NavItem[] {
  return [
    { text: "指南", link: "/zh-CN/guide/getting-started" },
    { text: "功能", link: "/zh-CN/guide/features" },
    {
      text: "开发者",
      items: [
        { text: "Native Mod 快速开始", link: "/zh-CN/guide/developer" },
        { text: "API 参考", link: "/zh-CN/api/mod" },
        { text: "从源码构建", link: "/zh-CN/guide/build" },
      ],
    },
    { text: "隐私政策", link: "/privacy-policy" },
    { text: "下载", link: releases },
    { text: "GitHub", link: repo },
  ];
}

export default defineConfig({
  title: "LeviLauncher",
  description: "Documentation for the LeviLaunchroid Android Minecraft Bedrock launcher.",
  lang: "en-US",
  base,
  cleanUrls: true,
  lastUpdated: true,
  head: [
    ["link", { rel: "icon", href: `${base}appicon.png` }],
    ["meta", { name: "theme-color", content: "#16a34a" }],
  ],
  themeConfig: {
    logo: "/appicon.png",
    search: {
      provider: "local",
    },
    socialLinks: [{ icon: "github", link: repo }],
    footer: {
      copyright: "Copyright © 2024-2026 LeviMC",
    },
  },
  locales: {
    root: {
      label: "English",
      lang: "en-US",
      link: "/",
      themeConfig: {
        nav: buildEnglishNav(),
        sidebar: {
          "/guide/": buildEnglishSidebar("/"),
          "/api/": buildEnglishSidebar("/"),
        },
        outline: { level: [2, 3] },
        docFooter: { prev: "Previous page", next: "Next page" },
        editLink: {
          pattern: `${repo}/edit/main/docs/:path`,
          text: "Edit this page on GitHub",
        },
        returnToTopLabel: "Back to top",
        sidebarMenuLabel: "Menu",
        darkModeSwitchLabel: "Appearance",
        lightModeSwitchTitle: "Switch to light theme",
        darkModeSwitchTitle: "Switch to dark theme",
      },
    },
    "zh-CN": {
      label: "简体中文",
      lang: "zh-CN",
      link: "/zh-CN/",
      themeConfig: {
        nav: buildChineseNav(),
        sidebar: {
          "/zh-CN/guide/": buildChineseSidebar("/zh-CN/"),
          "/zh-CN/api/": buildChineseSidebar("/zh-CN/"),
        },
        outline: { level: [2, 3] },
        docFooter: { prev: "上一页", next: "下一页" },
        editLink: {
          pattern: `${repo}/edit/main/docs/:path`,
          text: "在 GitHub 上编辑此页",
        },
        returnToTopLabel: "返回顶部",
        sidebarMenuLabel: "菜单",
        darkModeSwitchLabel: "外观",
        lightModeSwitchTitle: "切换到浅色主题",
        darkModeSwitchTitle: "切换到深色主题",
      },
    },
  },
  markdown: {
    lineNumbers: true,
  },
  sitemap: {
    hostname: "https://levilaunchroid.levimc.org/",
  },
});
