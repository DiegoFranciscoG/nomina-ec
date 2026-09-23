import { defineConfig } from '@playwright/test';

/**
 * End-to-end tests against a running stack (docker compose up).
 * E2E_BASE_URL defaults to the compose web container; E2E_USER / E2E_PASSWORD are the demo credentials.
 * Locally it uses the installed Edge (PW_CHANNEL=msedge); in CI run `npx playwright install chromium` first.
 */
export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  timeout: 60_000,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://127.0.0.1:4200',
    channel: process.env['PW_CHANNEL'] || undefined,
    viewport: { width: 1440, height: 900 },
    locale: 'es-EC',
    screenshot: 'only-on-failure',
  },
});
