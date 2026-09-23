// Writes public/config.json from the API_URL environment variable (used by the Vercel build).
// The API URL is public information, not a secret; no keys ever reach the frontend.
import { writeFileSync } from 'node:fs';

const apiUrl = (process.env.API_URL ?? '').trim().replace(/\/+$/, '');
if (apiUrl && !/^https:\/\/[\w.-]+(:\d+)?$/.test(apiUrl)) {
  console.error(`API_URL must be an https origin without path, got "${apiUrl}"`);
  process.exit(1);
}
writeFileSync(new URL('../public/config.json', import.meta.url), `${JSON.stringify({ apiUrl }, null, 2)}\n`);
console.log(`config.json -> apiUrl = "${apiUrl || '(same origin)'}"`);
