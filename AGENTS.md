# Agents

This repo is OpenAPI-first. Update `openapi/api.yml` before changing client or server code.
This repo is also runtime-isolated with Devbox. Prefer `devbox run <script>` from repository root for local commands.

## Collaboration workflow
1. **Plan and research when needed**: For complex, high-risk, or unclear tasks (or anything involving dependency/version changes), think and research (web browsing allowed), then provide a concrete plan with reasons. Ask what the user does not understand and explain first. If the user wants changes, propose a revised plan. Only request to implement after a few rounds of clear, agreed reasons and steps. For simple, low-risk, clearly scoped tasks, proceed with a brief plan and post-change confirmation.
2. **Best-practice guidance**: If the user's approach is not best practice or details are unclear, suggest alternatives and ask for specifics. If details are unclear, offer a few options with purpose/why; iterate once or twice to converge, then implement.
3. **Testing expectations by scope**: After code changes, run relevant tests/verification aligned to the change scope:
   - Frontend small changes: `devbox run frontend-lint`, `devbox run frontend-test` (keep coverage at or above configured thresholds).
   - Frontend build-impacting changes: add `devbox run frontend-build`.
   - Frontend runtime/integration changes (API wiring, CSP, routing, Vite config): add `devbox run frontend-e2e` (Playwright).
   - Backend small changes: `devbox run backend-build` (or backend gradle test/build as appropriate).
   - Dependency or security-related changes: run `devbox run backend-dependency-check` and/or backend CycloneDX task.
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
- Backend: `devbox run backend-javadoc` and `devbox run backend-build`
- Frontend: `devbox run frontend-dev` (smoke), `devbox run frontend-build`, `devbox run frontend-lint`, `devbox run frontend-e2e`

## Commit gate
- For commits that touch frontend runtime/config behavior, run `devbox run frontend-verify-precommit` before `git commit`.

## Security tooling
- Dependency vulnerability scan: `devbox run backend-dependency-check`
- CycloneDX SBOM: backend Gradle task `cyclonedxBom` or `devbox run backend-security-report`
- Reports live under `backend/build/reports`
- Set `NVD_API_KEY` (from the NVD API key portal) in CI/local env for faster Dependency-Check scans.

## Release publishing
- Tag `v*` to trigger the release workflow.
- Frontend release asset: `frontend-dist.zip`.
- Backend publishes to GitHub Packages (Maven) via `devbox run backend-publish` (or direct Gradle command in CI).
