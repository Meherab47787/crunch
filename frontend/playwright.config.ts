import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests run against a live stack.
 *  - Local dev:   backend on :8080 + `npm start` (ng serve with proxy) → E2E_BASE_URL=http://localhost:4200
 *  - Docker:      `docker compose up` → E2E_BASE_URL=http://localhost:8081
 * Staff tests need the bootstrap accounts: E2E_ADMIN_EMAIL / E2E_ADMIN_PASSWORD, E2E_KITCHEN_EMAIL / E2E_KITCHEN_PASSWORD.
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list'], ['html', { outputFolder: 'e2e/playwright-report', open: 'never' }]],
  outputDir: 'e2e/test-results',
  use: {
    baseURL: process.env['E2E_BASE_URL'] ?? 'http://localhost:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'mobile', use: { ...devices['Pixel 7'] }, testMatch: /ordering\.spec\.ts/ },
  ],
});
