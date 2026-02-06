import { defineConfig } from '@playwright/test';

const FRONTEND_PORT = 4173;
const BACKEND_PORT = 8080;
const isWindows = process.platform === 'win32';
const OUTPUT_ROOT = '../output/playwright';
const REPORT_HTML_DIR = `${OUTPUT_ROOT}/html-report`;
const REPORT_JSON_FILE = `${OUTPUT_ROOT}/report.json`;
const REPORT_JUNIT_FILE = `${OUTPUT_ROOT}/report.xml`;
const TEST_RESULTS_DIR = `${OUTPUT_ROOT}/test-results`;

const backendCommand = isWindows
  ? '..\\backend\\gradlew.bat --project-dir ..\\backend bootRun'
  : '../backend/gradlew --project-dir ../backend bootRun';

/**
 * Playwright end-to-end test configuration.
 *
 * <pre>
 * Responsibilities:
 * 1) Start backend and preview servers before browser tests.
 * 2) Execute tests against production-like frontend assets.
 * 3) Preserve artifacts (trace/screenshot/video) for failed cases.
 * </pre>
 */
export default defineConfig({
  testDir: './e2e',
  outputDir: TEST_RESULTS_DIR,
  timeout: 30000,
  expect: {
    timeout: 10000,
  },
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI
    ? [
        ['github'],
        ['html', { open: 'never', outputFolder: REPORT_HTML_DIR }],
        ['json', { outputFile: REPORT_JSON_FILE }],
        ['junit', { outputFile: REPORT_JUNIT_FILE }],
      ]
    : [
        ['list'],
        ['html', { open: 'never', outputFolder: REPORT_HTML_DIR }],
        ['json', { outputFile: REPORT_JSON_FILE }],
      ],
  use: {
    baseURL: `http://127.0.0.1:${FRONTEND_PORT}`,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  webServer: [
    {
      command: backendCommand,
      url: `http://127.0.0.1:${BACKEND_PORT}/api/hello`,
      timeout: 120000,
      reuseExistingServer: !process.env.CI,
      stdout: 'pipe',
      stderr: 'pipe',
    },
    {
      command: `npm run build && npm run preview -- --host 127.0.0.1 --port ${FRONTEND_PORT}`,
      url: `http://127.0.0.1:${FRONTEND_PORT}`,
      timeout: 120000,
      reuseExistingServer: !process.env.CI,
      stdout: 'pipe',
      stderr: 'pipe',
    },
  ],
});
