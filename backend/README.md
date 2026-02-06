# Backend

Spring Boot service built with Gradle in an OpenAPI-first workflow.

## Structure
- `build.gradle` configures dependencies and build tooling.
- `src/main/java` contains application code.
- `src/test/java` contains backend tests.
- `build/generated` contains OpenAPI-generated sources (do not edit).
- `config/` holds formatter and quality tool configuration.

## Prerequisites
- `devbox` (recommended)
- Optional fallback only: Java 21+ and Gradle wrapper

## Common Commands (Devbox-First)
```bash
devbox run backend-javadoc
devbox run backend-build
devbox run backend-boot
devbox run backend-dependency-check
devbox run backend-security-report
```

## Taskfile shortcuts
```bash
task build
task test
task javadoc
task check
```
Use `devbox run ...` as the primary entrypoint for runtime isolation. Taskfile and raw Gradle commands remain available as fallback.

## OpenAPI generation
`openApiGenerate` runs as part of the build and generates API interfaces/models from `../openapi/api.yml`.

## Formatting and quality
- Google Java format via `config/formatter/formatter.xml`.
- Checkstyle/PMD/SpotBugs/JaCoCo run in backend Gradle checks.
- OpenAPI-generated code is excluded from formatting and quality checks.

## Code style note
- Prefer Lombok annotations to avoid hand-written Java boilerplate (constructors, getters/setters, builders).

## Debug and troubleshooting
```bash
task build:debug
task test:debug
task javadoc:debug
```
