// LeviLaunchroid documentation (https://docs.levimc.org/LeviLaunchroid/), built with the vendored levimc-kit.
// Port of the former VitePress config: same URLs (cleanUrls), locales, nav, sidebars and labels.
import { defineDocsConfig, type NavEntry } from './kit/docs/node/config.ts';

const repo = 'https://github.com/LiteLDev/LeviLaunchroid';
const releases = `${repo}/releases`;
// Deploy under another path (e.g. a fork's GitHub Pages) with DOCS_BASE=/<path>/ (VITEPRESS_BASE still works).
// with-base.mjs applies the same rule to the preview / linkcheck / smoke scripts: keep the two in sync.
const base = process.env.DOCS_BASE ?? process.env.VITEPRESS_BASE ?? '/LeviLaunchroid/';

function buildEnglishSidebar(localePrefix: string): NavEntry[] {
  return [
    {
      text: 'Guide',
      items: [
        { text: 'Getting Started', link: `${localePrefix}guide/getting-started` },
        { text: 'Features', link: `${localePrefix}guide/features` },
        { text: 'Compatibility', link: `${localePrefix}guide/compatibility` },
      ],
    },
    {
      text: 'Developer',
      items: [
        { text: 'Native Mod Quick Start', link: `${localePrefix}guide/developer` },
        { text: 'Build from Source', link: `${localePrefix}guide/build` },
        { text: 'Mod Entry API', link: `${localePrefix}api/mod` },
        { text: 'Mod Menu API', link: `${localePrefix}api/mod-menu` },
        { text: 'Config API', link: `${localePrefix}api/config` },
        { text: 'Input API', link: `${localePrefix}api/input` },
        { text: 'Hook API', link: `${localePrefix}api/hook` },
        { text: 'Signature API', link: `${localePrefix}api/signature` },
        { text: 'Patch API', link: `${localePrefix}api/patch` },
        { text: 'Memory Hook Macros', link: `${localePrefix}api/memory-hook-macros` },
        { text: 'Types and Macros', link: `${localePrefix}api/types-and-macros` },
      ],
    },
  ];
}

function buildChineseSidebar(localePrefix: string): NavEntry[] {
  return [
    {
      text: '指南',
      items: [
        { text: '快速开始', link: `${localePrefix}guide/getting-started` },
        { text: '功能概览', link: `${localePrefix}guide/features` },
        { text: '兼容性与常见问题', link: `${localePrefix}guide/compatibility` },
      ],
    },
    {
      text: '开发者',
      items: [
        { text: 'Native Mod 快速开始', link: `${localePrefix}guide/developer` },
        { text: '从源码构建', link: `${localePrefix}guide/build` },
        { text: 'Mod 入口 API', link: `${localePrefix}api/mod` },
        { text: 'Mod Menu API', link: `${localePrefix}api/mod-menu` },
        { text: 'Config API', link: `${localePrefix}api/config` },
        { text: 'Input API', link: `${localePrefix}api/input` },
        { text: 'Hook API', link: `${localePrefix}api/hook` },
        { text: 'Signature API', link: `${localePrefix}api/signature` },
        { text: 'Patch API', link: `${localePrefix}api/patch` },
        { text: 'Memory Hook 宏', link: `${localePrefix}api/memory-hook-macros` },
        { text: 'Types 与宏', link: `${localePrefix}api/types-and-macros` },
      ],
    },
  ];
}

function buildEnglishNav(): NavEntry[] {
  return [
    { text: 'Guide', link: '/guide/getting-started' },
    { text: 'Features', link: '/guide/features' },
    {
      text: 'Developer',
      items: [
        { text: 'Native Mod Quick Start', link: '/guide/developer' },
        { text: 'API Reference', link: '/api/mod' },
        { text: 'Build from Source', link: '/guide/build' },
      ],
    },
    { text: 'Privacy Policy', link: '/privacy-policy' },
    { text: 'Downloads', link: releases },
  ];
}

function buildChineseNav(): NavEntry[] {
  return [
    { text: '指南', link: '/zh-CN/guide/getting-started' },
    { text: '功能', link: '/zh-CN/guide/features' },
    {
      text: '开发者',
      items: [
        { text: 'Native Mod 快速开始', link: '/zh-CN/guide/developer' },
        { text: 'API 参考', link: '/zh-CN/api/mod' },
        { text: '从源码构建', link: '/zh-CN/guide/build' },
      ],
    },
    // The privacy policy is English-only: link straight to it (no zh-CN page exists).
    { text: '隐私政策', link: '/privacy-policy' },
    { text: '下载', link: releases },
  ];
}

export default defineDocsConfig({
  base,
  title: 'LeviLauncher',
  description: 'Documentation for the LeviLaunchroid Android Minecraft Bedrock launcher.',
  project: 'LeviLaunchroid',
  logo: { src: 'appicon.png', alt: 'LeviLauncher' },
  favicon: 'appicon.png',
  repo,
  contentDir: '.',
  i18n: {
    mode: 'folder',
    defaultLocale: 'root',
    locales: [
      { code: 'root', label: 'English', lang: 'en-US', prefix: '' },
      { code: 'zh-CN', label: '简体中文', lang: 'zh-CN', prefix: 'zh-CN/' },
    ],
  },
  urlStyle: 'html',
  cleanUrls: true,
  slug: 'vitepress',
  markdown: { compat: 'commonmark', lineNumbers: true },
  outline: [2, 3],
  lastUpdated: true,
  editLink: {
    pattern: `${repo}/edit/main/docs/:path`,
    text: { root: 'Edit this page on GitHub', 'zh-CN': '在 GitHub 上编辑此页' },
  },
  nav: { root: buildEnglishNav(), 'zh-CN': buildChineseNav() },
  sidebar: {
    root: {
      '/guide/': buildEnglishSidebar('/'),
      '/api/': buildEnglishSidebar('/'),
    },
    'zh-CN': {
      '/zh-CN/guide/': buildChineseSidebar('/zh-CN/'),
      '/zh-CN/api/': buildChineseSidebar('/zh-CN/'),
    },
  },
  social: [{ icon: 'github', link: repo }],
  ui: {
    root: { previous: 'Previous page', next: 'Next page', backToTop: 'Back to top', menu: 'Menu' },
    'zh-CN': { previous: '上一页', next: '下一页', backToTop: '返回顶部', menu: '菜单' },
  },
  hostname: 'https://docs.levimc.org',
  sitemap: { hostname: 'https://docs.levimc.org' },
});
