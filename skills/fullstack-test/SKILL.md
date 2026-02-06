---
name: fullstack-test
description: Add or update frontend/backend tests with this repository's OpenAPI-first flow, strict quality gates, and current folder conventions.
---

# Fullstack Test Workflow (Repository-Aligned)

Use this skill when the task involves adding or updating frontend (`frontend`) or backend (`backend`) tests.

## Rules (must follow)

1. **OpenAPI-first for contract changes**:
   - Update `openapi/api.yml` before changing API contracts.
   - Regenerate frontend API types with `cd frontend && npm run generate` when needed.
   - Do not hand-edit generated code (`frontend/src/api/generated.ts`, `backend/build/generated/**`, generated API/model/invoker packages).
2. **Use current repository layout**:
   - Frontend tests: colocated under `frontend/src` using `*.test.ts` / `*.test.tsx`.
   - Backend tests: `backend/src/test/java/com/example/demo/**`, grouped by package/domain.
3. **Prefer test-first when practical**:
   - Write or update tests before implementation for behavior changes.
   - Small deterministic refactors may update tests in the same change set.
4. **Keep comments concise and English-only**:
   - Avoid long block comments in Java tests; PMD `CommentSize` can fail on long lines.
   - Use clear test names and short intent comments instead of verbose templates.
5. **Respect strict quality gates**:
   - Backend `build` includes PMD/Checkstyle/SpotBugs/JaCoCo checks.
   - JaCoCo coverage thresholds are strict; include branch-path tests for new logic.

## Workflow

1. **Plan test placement**:
   - Pick the target module (`frontend` or `backend`) and colocated test file path.
2. **Write/update tests first when feasible**:
   - Frontend: Jest + Testing Library; mock API client/fetch boundaries.
   - Backend: JUnit/Spring tests; cover success, validation, and failure branches.
3. **Implement/adjust code** after test intent is clear.
4. **Regenerate contracts if needed**:
   - Run generation commands when OpenAPI schema changes.
5. **Verify locally with repo commands**:
   - Frontend (small change): `cd frontend && npm run lint && npm test -- --watch=false`
   - Frontend (build-impacting): add `cd frontend && npm run build`
   - Backend (small change): `cd backend && ./gradlew test`
   - Backend (pre-share baseline): `cd backend && ./gradlew spotlessApply javadoc build`
   - Security/dependency scope: `cd backend && ./gradlew dependencyCheckAnalyze` and/or `./gradlew cyclonedxBom`

## Completion Checklist

1. Tests are added/updated in the correct module and folder.
2. No generated file is manually edited.
3. Relevant verification commands passed for the change scope.
4. If a check is skipped, document exactly which command was skipped and why.
