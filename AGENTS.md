# Agents

This repo is OpenAPI-first. Update `openapi/api.yml` before changing client or server code.

## Standards
- Write comments in English.
- Keep generated code isolated and reproducible.
- Do not hand-edit OpenAPI-generated code; keep manual changes in non-generated sources.
- OpenAPI-generated code is excluded from formatting and quality checks.
- Format-on-save is enabled: Java uses `backend/config/formatter/formatter.xml` (Google style), frontend uses `frontend/.prettierrc.json`.
- For dependency versions and CI/CD action versions, always verify online that the version is current, available, and compatible before updating.

## Checks (before sharing changes)
- Backend: `./gradlew javadoc` and `./gradlew build`
- Frontend: `npm run dev` (smoke), `npm run build`, `npm run lint`

## Security tooling
- Dependency vulnerability scan: `./gradlew dependencyCheckAnalyze`
- CycloneDX SBOM: `./gradlew cyclonedxBom` or `./gradlew securityReport`
- Reports live under `backend/build/reports`
- Set `NVD_API_KEY` (from the NVD API key portal) in CI/local env for faster Dependency-Check scans.

## Release publishing
- Tag `v*` to trigger the release workflow.
- Frontend release asset: `frontend-dist.zip`.
- Backend publishes to GitHub Packages (Maven) via `./gradlew publish`.
