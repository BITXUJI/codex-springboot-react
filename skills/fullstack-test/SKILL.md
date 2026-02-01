---
name: fullstack-test
description: Write frontend/back-end tests first, with structured test annotations and end-to-end-ish controller-to-service coverage using mocks/spies for dependencies.
---

# Fullstack Test Workflow

Use this skill when the task involves adding or updating frontend (Jest/RTL) or backend (JUnit/Spring) tests.

## Rules (must follow)

1. **Test-first**: write tests before implementation code.
2. **Structured test annotations**: every test must include a top comment with:
   - Theme (first line)
   - Test view
   - Test conditions
   - Test result
   For Java, use `/* ... */` and allow `<pre>` to format if needed.
3. **Test location planning**: decide the target folder and module before writing tests. Keep tests grouped by feature/domain.
4. **Prefer combined coverage**: for a new API (controller + service), write tests that start at controller validation and cover through service logic. For service dependencies, use mocks/spies.
5. **If comment length violates PMD**: adjust the PMD rule in the repo PMD config so the annotation is allowed.

## Workflow

1. **Pick test placement**: choose module + folder structure (e.g., `frontend/src/.../__tests__` or `backend/src/test/java/...`), aligned to feature.
2. **Write tests first**:
   - Frontend: Jest + Testing Library; mock network or API client.
   - Backend: JUnit/Spring; start at controller tests and cover validation + service logic with mocked dependencies.
3. **Implement code** only after tests are in place.
4. **If PMD complains about comment length**:
   - Locate the PMD config in the repo (search for `pmd` config files).
   - Update only the relevant rule to permit the required annotation length.
5. **Verify**: run the relevant test commands locally.

## Comment Template

Use this in every test (front or back), at the top of the test body.

```text
Theme: <short topic, first line only>
Test view: <what behavior is being validated>
Test conditions: <inputs, setup, constraints>
Test result: <expected outcome>
```

For Java:

```java
/*
Theme: ...
Test view: ...
Test conditions: ...
Test result: ...
<pre>
optional formatted details
</pre>
*/
```
