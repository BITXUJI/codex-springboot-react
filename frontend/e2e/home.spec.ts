import { expect, test } from '@playwright/test';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

test('renders hello message without browser/runtime failures', async ({ page }) => {
  const outputRoot = path.resolve(process.cwd(), '..', 'output', 'playwright');
  const snapshotDir = path.join(outputRoot, 'snapshots');
  const screenshotPath = path.join(snapshotDir, 'home.png');
  const summaryPath = path.join(outputRoot, 'home-summary.json');
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
        consoleErrorCount: consoleErrors.length,
        pageErrorCount: pageErrors.length,
        failedRequestCount: failedRequests.length,
      },
      null,
      2,
    ),
    'utf8',
  );

  expect(consoleErrors, consoleErrors.join('\n')).toEqual([]);
  expect(pageErrors, pageErrors.join('\n')).toEqual([]);
  expect(failedRequests, failedRequests.join('\n')).toEqual([]);
});
