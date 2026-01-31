# Agents

This project is OpenAPI-first. Always update `openapi/api.yml` before touching the server or client.

## Conventions
- Write comments in English.
- Keep code generation deterministic.
- Prefer small, readable diffs.

## Checks
- Backend: `./gradlew clean build`
- Frontend: `npm run lint` and `npm test`
