# codex-springboot-react

This repo contains a minimal Spring Boot + React (Vite + TypeScript) setup with an OpenAPI-first workflow.

## Structure
- `openapi/api.yml` is the source of truth for the HTTP API.
- `backend/` is a Spring Boot + Gradle service that implements the API.
- `frontend/` is a Vite React app that calls the API.

## Prerequisites
- Java 21+
- Gradle (local install is OK)
- Node 18+ and npm

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

## Local dev flow
1. Start the backend: `./gradlew bootRun` (port 8080).
2. Start the frontend: `npm run dev` (port 5173).
3. Open the app; it calls `GET /api/hello` and renders the response.
