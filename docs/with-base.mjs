// Runs a kit script with the site's base path substituted for every "{base}" in its arguments, so
// `npm run preview|linkcheck|smoke` follow DOCS_BASE / VITEPRESS_BASE like the build does.
// Usage: node with-base.mjs <script> [args…]
import { spawnSync } from 'node:child_process';

// Keep in sync with `base` in docs.config.ts.
const base = process.env.DOCS_BASE ?? process.env.VITEPRESS_BASE ?? '/LeviLaunchroid/';
const [script, ...args] = process.argv.slice(2);
if (!script) {
  console.error('Usage: node with-base.mjs <script> [args…]');
  process.exit(2);
}
const run = spawnSync(process.execPath, [script, ...args.map((a) => a.replaceAll('{base}', base))], { stdio: 'inherit' });
if (run.error) throw run.error;
process.exit(run.status ?? 1);
