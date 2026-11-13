import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e', timeout: 30000, workers: 1, retries: 0,
  use: { baseURL: 'http://127.0.0.1:5173', channel: 'chrome', headless: true, timezoneId: 'Asia/Bangkok', screenshot: 'only-on-failure' },
});
