# OpenAPI

API definitions for the project (source of truth).

## Structure
- `api.yml` defines all HTTP endpoints, schemas, and contracts.

## Workflow
- Update `api.yml` first.
- Backend and frontend clients are generated from this file.

## Common commands
```bash
# Generate backend interfaces/models
cd ../backend && ./gradlew openApiGenerate

# Generate frontend client types
cd ../frontend && npm run generate
```
