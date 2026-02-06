# Frontend

Vite + React + TypeScript frontend that calls the Spring Boot API.

## Structure
- `src/` contains app and API client code.
- `src/api/generated.ts` is OpenAPI-generated (do not edit).
- `src/setupTests.ts` configures Jest DOM matchers.

## Prerequisites
- `devbox` (recommended)
- Optional fallback only: Node 22+, npm, and Java 21+

## Common Commands (Devbox-First)
```bash
devbox run frontend-install
devbox run frontend-dev
devbox run frontend-generate
devbox run frontend-build
devbox run frontend-lint
devbox run frontend-test
devbox run frontend-e2e
```

## Taskfile shortcuts
```bash
task install
task dev
task build
task lint
task test
task test:e2e
task verify:precommit
```
Use `devbox run ...` as the primary entrypoint for runtime isolation. Taskfile and raw npm commands remain available as fallback.

## OpenAPI generation
`devbox run frontend-generate` regenerates `src/api/generated.ts` from `../openapi/api.yml`.

## Testing
```bash
task test:coverage
```

## Playwright end-to-end regression
```bash
# One-time browser setup
devbox run frontend-e2e-install

# Runs backend + frontend preview automatically, then executes e2e tests
devbox run frontend-e2e

# Pre-commit quality gate for frontend runtime/config changes
devbox run frontend-verify-precommit
```

`devbox run frontend-e2e` validates:
- Page renders expected UI.
- `/api/hello` flow works against the backend.
- Browser console/page/request failure signals remain clean.
- Preview response contains strict CSP without `unsafe-inline` and `unsafe-eval`.

Artifacts are written to `../output/playwright/`:
- `report.json` (Playwright JSON report)
- `html-report/` (Playwright HTML report)
- `home-summary.json` and `snapshots/home.png` (custom regression snapshot)

In GitHub Actions, the `frontend-e2e` job uploads this folder as artifact `playwright-output`.
The workflow summary also includes:
- Artifact download URL.
- Inline preview of `snapshots/home.png` (rendered from base64 in the summary).
