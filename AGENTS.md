# Agents

This repo is OpenAPI-first. Update `openapi/api.yml` before changing client or server code.

## Standards
- Write comments in English.
- Keep generated code isolated and reproducible.
- Do not hand-edit OpenAPI-generated code; keep manual changes in non-generated sources.
- OpenAPI-generated code is excluded from formatting and quality checks.
- Format-on-save is enabled: Java uses `backend/config/formatter/formatter.xml` (Google style), frontend uses `frontend/.prettierrc.json`.

## Checks (before sharing changes)
- Backend: `./gradlew javadoc` and `./gradlew build`
- Frontend: `npm run dev` (smoke), `npm run build`, `npm run lint`
