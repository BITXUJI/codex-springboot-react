import { expect, test, type Page } from '@playwright/test';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

function collectPageIssues(page: Page) {
  const consoleErrors: string[] = [];
  const pageErrors: string[] = [];
  const failedRequests: string[] = [];

  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text());
    }
  });

  page.on('pageerror', (error) => {
    pageErrors.push(error.message);
  });

  page.on('requestfailed', (request) => {
    const reason = request.failure()?.errorText ?? 'unknown';
    failedRequests.push(`${request.method()} ${request.url()} ${reason}`);
  });

  return { consoleErrors, pageErrors, failedRequests };
}

test('renders hello message without browser/runtime failures', async ({ page }) => {
  const outputRoot = path.resolve(process.cwd(), '..', 'output', 'playwright');
  const snapshotDir = path.join(outputRoot, 'snapshots');
  const screenshotPath = path.join(snapshotDir, 'home.png');
  const summaryPath = path.join(outputRoot, 'home-summary.json');
  const diagnostics = collectPageIssues(page);

  const documentResponse = await page.goto('/');
  expect(documentResponse).not.toBeNull();

  const cspHeader =
    documentResponse?.headers()['content-security-policy'] ?? '';
  expect(cspHeader).toContain("default-src 'self'");
  expect(cspHeader).toContain("script-src 'self'");
  expect(cspHeader).not.toContain('unsafe-inline');
  expect(cspHeader).not.toContain('unsafe-eval');

  await expect(
    page.getByRole('heading', { name: 'Codex React + Spring Boot' }),
  ).toBeVisible();
  await expect(page.getByText('Hello from Spring Boot')).toBeVisible();
  await expect(page.getByRole('alert')).toHaveCount(0);

  await mkdir(snapshotDir, { recursive: true });
  await page.screenshot({ path: screenshotPath, fullPage: true });
  await writeFile(
    summaryPath,
    JSON.stringify(
      {
        url: page.url(),
        cspHeader,
        screenshotPath,
        consoleErrorCount: diagnostics.consoleErrors.length,
        pageErrorCount: diagnostics.pageErrors.length,
        failedRequestCount: diagnostics.failedRequests.length,
      },
      null,
      2,
    ),
    'utf8',
  );

  expect(diagnostics.consoleErrors, diagnostics.consoleErrors.join('\n')).toEqual([]);
  expect(diagnostics.pageErrors, diagnostics.pageErrors.join('\n')).toEqual([]);
  expect(diagnostics.failedRequests, diagnostics.failedRequests.join('\n')).toEqual([]);
});

test('shows backend message when /api/hello returns structured 404 error', async ({ page }) => {
  const diagnostics = collectPageIssues(page);
  await page.route('**/api/hello', async (route) => {
    await route.fulfill({
      status: 404,
      contentType: 'application/json',
      headers: { 'X-Request-Id': 'trace-e2e-404' },
      body: JSON.stringify({
        code: 'NOT_FOUND',
        message: 'Not found',
        traceId: 'trace-e2e-404',
        timestamp: '2026-02-08T00:00:00Z',
        path: '/api/hello',
      }),
    });
  });

  await page.goto('/');
  await expect(page.getByRole('alert')).toHaveText('Not found');
  await expect(page.getByText('Loading...')).toHaveCount(0);
  expect(diagnostics.consoleErrors.join('\n')).toContain(
    'Failed to load resource: the server responded with a status of 404',
  );
  expect(diagnostics.pageErrors, diagnostics.pageErrors.join('\n')).toEqual([]);
  expect(diagnostics.failedRequests, diagnostics.failedRequests.join('\n')).toEqual([]);
});

test('falls back to generic HTTP message for non-JSON error payload', async ({ page }) => {
  const diagnostics = collectPageIssues(page);
  await page.route('**/api/hello', async (route) => {
    await route.fulfill({
      status: 502,
      contentType: 'text/plain',
      body: 'Bad Gateway',
    });
  });

  await page.goto('/');
  await expect(page.getByRole('alert')).toHaveText('Request failed: 502');
  expect(diagnostics.consoleErrors.join('\n')).toContain(
    'Failed to load resource: the server responded with a status of 502',
  );
  expect(diagnostics.pageErrors, diagnostics.pageErrors.join('\n')).toEqual([]);
  expect(diagnostics.failedRequests, diagnostics.failedRequests.join('\n')).toEqual([]);
});

test('backend non-existent API returns expected not-found error shape example', async ({
  request,
}) => {
  const missingPath = '/api/e2e-not-found-example';
  const response = await request.get(`http://127.0.0.1:8080${missingPath}`, {
    headers: { Accept: 'application/json' },
  });

  expect(response.status()).toBe(404);
  const contentType = response.headers()['content-type'] ?? '';
  expect(contentType).toContain('application/json');
  const payload = (await response.json()) as {
    code?: string;
    message?: string;
    traceId?: string;
    timestamp?: string;
    path?: string;
  };
  expect(payload.code).toBe('NOT_FOUND');
  expect(payload.message).toBe('Not found');
  expect(payload.traceId).toBeTruthy();
  expect(payload.timestamp).toBeTruthy();
  expect(payload.path).toBe(missingPath);
});
