# codex-springboot-react

This repo contains a minimal Spring Boot + React (Vite + TypeScript) setup with an OpenAPI-first workflow.
Runtime isolation is managed by `devbox` at repository level.

## Structure
- `openapi/api.yml` is the source of truth for the HTTP API.
- `backend/` is a Spring Boot + Gradle service that implements the API.
- `frontend/` is a Vite React app that calls the API.
- `devbox.json` pins local runtime versions (Node and Java) and shared scripts.

## Prerequisites
- `devbox` (recommended)
- Optional fallback only: Java 21+, Node 22+, npm

## Quick Start (Devbox-First)
```bash
cd /Users/jixu/Desktop/codex-springboot-react
devbox install
devbox run doctor
```
First `devbox install` may trigger Nix installation and require interactive sudo on macOS.

See `/Users/jixu/Desktop/codex-springboot-react/docs/devbox.md` for details.

## Backend
```bash
devbox run backend-javadoc
devbox run backend-build
devbox run backend-boot
```

## Frontend
```bash
devbox run frontend-install
devbox run frontend-generate
devbox run frontend-build
devbox run frontend-lint
devbox run frontend-test
devbox run frontend-dev
```

## Playwright Regression
```bash
devbox run frontend-e2e-install
devbox run frontend-e2e
```

Generated artifacts are written to `output/playwright/`. In GitHub Actions, they are uploaded as artifact `playwright-output` in the `frontend-e2e` job.

## OpenAPI generation
- Backend: `openApiGenerate` runs during `build` to generate interfaces/models from `openapi/api.yml`.
- Frontend: `devbox run frontend-generate` uses `openapi-typescript` to generate a typed client.

## Formatting
- VS Code is configured for format-on-save via `.vscode/settings.json` and the devcontainer settings.
- Java formatting uses `backend/config/formatter/formatter.xml` (Google style) and is aligned with Checkstyle.
- Frontend formatting uses Prettier with `frontend/.prettierrc.json`.
- OpenAPI-generated code is excluded from formatting and quality checks; keep edits in non-generated files.

## Code style note
- Prefer Lombok annotations to avoid hand-written Java boilerplate (constructors, getters/setters, builders).

## Security and SBOM
- Dependency vulnerability scan: `devbox run backend-dependency-check` (reports in `backend/build/reports/dependency-check`).
- CycloneDX SBOM: run from backend Gradle tasks (output in `backend/build/reports`).
- Combined security report task: `devbox run backend-security-report`.
- For faster Dependency-Check scans, set `NVD_API_KEY` (from the NVD API key portal) as an environment variable.

## Release publishing
- Tag a release (e.g. `v1.0.0`) to trigger `.github/workflows/release.yml`.
- Frontend: builds `frontend/dist` and uploads `frontend-dist.zip` to the GitHub Release.
- Backend: publishes the Spring Boot jar to GitHub Packages (Maven); version is derived from the tag.
- GitHub Packages URL: `https://maven.pkg.github.com/BITXUJI/codex-springboot-react`
- Example tag commands:
  - `git tag -a v1.0.0 -m "release v1.0.0"`
  - `git push origin v1.0.0`

## Local dev flow
1. Start the backend: `devbox run backend-boot` (port 8080).
2. Start the frontend: `devbox run frontend-dev` (port 5173).
3. Open the app; it calls `GET /api/hello` and renders the response.

## Debug and troubleshooting commands
- Runtime check: `devbox run doctor`
- Backend build with full warnings: `cd backend && task build:debug`
- Backend Javadoc with full warnings: `cd backend && task javadoc:debug`
- Backend tests with full warnings: `cd backend && task test:debug`
- Frontend tests with coverage: `cd frontend && task test:coverage`
- CI workflows locally: `act -W .github/workflows/ci.yml`

## Common Git commands
- Check status: `git status -sb`
- View staged diff: `git diff --staged`
- View working tree diff: `git diff`
- Commit: `git commit -m "message"`
- Push: `git push`
