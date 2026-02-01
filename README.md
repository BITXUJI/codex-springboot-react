# codex-springboot-react

This repo contains a minimal Spring Boot + React (Vite + TypeScript) setup with an OpenAPI-first workflow.

## Structure
- `openapi/api.yml` is the source of truth for the HTTP API.
- `backend/` is a Spring Boot + Gradle service that implements the API.
- `frontend/` is a Vite React app that calls the API.

## Prerequisites
- Java 21+
- Gradle (local install is OK)
- Node 22+ and npm

## Backend
```bash
cd backend
# Generates code from OpenAPI and runs static checks + tests
./gradlew clean build
```

## Frontend
```bash
cd frontend
# Install deps, generate API client, then run checks
npm install
npm run generate
npm run lint
npm test
npm run dev
```

## OpenAPI generation
- Backend: `openApiGenerate` runs during `build` to generate interfaces/models from `openapi/api.yml`.
- Frontend: `npm run generate` uses `openapi-typescript` to generate a typed client.

## Formatting
- VS Code is configured for format-on-save via `.vscode/settings.json` and the devcontainer settings.
- Java formatting uses `backend/config/formatter/formatter.xml` (Google style) and is aligned with Checkstyle.
- Frontend formatting uses Prettier with `frontend/.prettierrc.json`.
- OpenAPI-generated code is excluded from formatting and quality checks; keep edits in non-generated files.

## Code style note
- Prefer Lombok annotations to avoid hand-written Java boilerplate (constructors, getters/setters, builders).

## Security and SBOM
- Dependency vulnerability scan: `./gradlew dependencyCheckAnalyze` (reports in `backend/build/reports/dependency-check`).
- CycloneDX SBOM: `./gradlew cyclonedxBom` (output in `backend/build/reports`).
- Combined security report task: `./gradlew securityReport`.
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
1. Start the backend: `./gradlew bootRun` (port 8080).
2. Start the frontend: `npm run dev` (port 5173).
3. Open the app; it calls `GET /api/hello` and renders the response.

## Debug and troubleshooting commands
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
