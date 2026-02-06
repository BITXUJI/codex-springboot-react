# Agents

This repo is OpenAPI-first. Update `openapi/api.yml` before changing client or server code.

## Collaboration workflow
1. **Plan and research when needed**: For complex, high-risk, or unclear tasks (or anything involving dependency/version changes), think and research (web browsing allowed), then provide a concrete plan with reasons. Ask what the user does not understand and explain first. If the user wants changes, propose a revised plan. Only request to implement after a few rounds of clear, agreed reasons and steps. For simple, low-risk, clearly scoped tasks, proceed with a brief plan and post-change confirmation.
2. **Best-practice guidance**: If the user's approach is not best practice or details are unclear, suggest alternatives and ask for specifics. If details are unclear, offer a few options with purpose/why; iterate once or twice to converge, then implement.
3. **Testing expectations by scope**: After code changes, run relevant tests/verification aligned to the change scope:
   - Frontend small changes: `npm run lint`, `npm test` (keep coverage at or above configured thresholds).
   - Frontend build-impacting changes: add `npm run build`.
   - Backend small changes: `./gradlew test` or `./gradlew build` (as appropriate).
   - Dependency or security-related changes: run `./gradlew dependencyCheckAnalyze` and/or `./gradlew cyclonedxBom`.
   Ensure no regressions, no violations of static checks, and coverage does not drop below required thresholds.

## Standards
- Write comments in English.
- For manual Javadoc/JSDoc updates, keep the first line as a short summary and add a concrete English procedure under it using `<pre>...</pre>` (for example: `Algorithm`, `Responsibilities`, or `Usage` with numbered steps).
- Apply the Javadoc/JSDoc expansion to hand-written source files by default; keep existing test comments unchanged unless explicitly requested.
- Keep generated code isolated and reproducible.
- Do not hand-edit OpenAPI-generated code; keep manual changes in non-generated sources.
- OpenAPI-generated code is excluded from formatting and quality checks.
- Java formatting uses `backend/config/formatter/formatter.xml`; after Java changes, run `./gradlew spotlessApply`.
- Frontend formatting uses `frontend/.prettierrc.json`.
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
