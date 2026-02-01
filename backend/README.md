# Backend

Spring Boot service built with Gradle in an OpenAPI-first workflow.

## Structure
- `build.gradle` configures dependencies and build tooling.
- `src/main/java` contains application code.
- `src/test/java` contains backend tests.
- `build/generated` contains OpenAPI-generated sources (do not edit).
- `config/` holds formatter and quality tool configuration.

## Prerequisites
- Java 21+
- Gradle (wrapper included)

## Common commands
```bash
./gradlew clean build
./gradlew test
./gradlew javadoc
./gradlew bootRun
```

## Taskfile shortcuts
```bash
task build
task test
task javadoc
task check
```
Use either Gradle commands or the Taskfile shortcuts; they map to the same workflows.

## OpenAPI generation
`openApiGenerate` runs as part of the build and generates API interfaces/models from `../openapi/api.yml`.

## Formatting and quality
- Google Java format via `config/formatter/formatter.xml`.
- Checkstyle/PMD/SpotBugs/JaCoCo run in `./gradlew check`.
- OpenAPI-generated code is excluded from formatting and quality checks.

## Code style note
- Prefer Lombok annotations to avoid hand-written Java boilerplate (constructors, getters/setters, builders).

## Debug and troubleshooting
```bash
task build:debug
task test:debug
task javadoc:debug
```
