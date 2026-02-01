# Frontend

Vite + React + TypeScript frontend that calls the Spring Boot API.

## Structure
- `src/` contains app and API client code.
- `src/api/generated.ts` is OpenAPI-generated (do not edit).
- `src/setupTests.ts` configures Jest DOM matchers.

## Prerequisites
- Node 22+ and npm

## Common commands
```bash
npm ci
npm run dev
npm run generate
npm run build
npm run lint
npm test
```

## Taskfile shortcuts
```bash
task install
task dev
task build
task lint
task test
```

## OpenAPI generation
`npm run generate` regenerates `src/api/generated.ts` from `../openapi/api.yml`.

## Testing
```bash
task test:coverage
```
