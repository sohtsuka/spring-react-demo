# Repository Guidelines

## Project Structure & Module Organization

This repository contains a Spring Boot API in `backend/` and a Vite React SPA in `frontend/`. Backend Java sources live in `backend/src/main/java/com/example/app`, with resources, Flyway migrations, and MyBatis XML mappers under `backend/src/main/resources`. Backend tests mirror the package layout in `backend/src/test/java`. Frontend source is in `frontend/src`, grouped by `api`, `components`, `hooks`, `lib`, `pages`, `schemas`, and `types`; Vitest tests use `__tests__/` folders or `*.test.tsx`. Playwright E2E tests live in `frontend/e2e`. Local PostgreSQL is defined by `compose.yaml`.

## Build, Test, and Development Commands

- `podman-compose up -d` or `docker compose up -d`: start the local PostgreSQL service from the repository root.
- `cd backend && ./gradlew bootRun --args='--spring.profiles.active=dev'`: run the API on `localhost:8080`.
- `cd backend && ./gradlew spotlessCheck checkstyleMain checkstyleTest test spotbugsMain jacocoTestCoverageVerification`: run backend CI.
- `cd backend && ./gradlew spotlessApply`: auto-format backend Java.
- `cd frontend && pnpm install --frozen-lockfile`: install frontend dependencies.
- `cd frontend && pnpm dev`: run the SPA on `localhost:5173`.
- `cd frontend && pnpm build`: type-check and build production assets.
- `cd frontend && pnpm lint && pnpm test`: run frontend linting and unit/component tests.
- `cd frontend && pnpm e2e`: run Playwright tests after backend and frontend are running.
- `cd backend && ./gradlew pitest` / `cd frontend && pnpm mutation`: run mutation tests (PIT / StrykerJS). They are slow, so CI runs them weekly and on demand in the `Mutation test` workflow; reports go to `backend/build/reports/pitest/` and `frontend/reports/mutation/`. Surviving mutants show behavior the tests execute but never assert.

## Language Runtime LTS Policy (Mandatory)

- For languages/runtimes with official LTS releases, use only currently supported LTS release lines. Never switch to a non-LTS, Current, early-access, or preview release merely because its version number is higher.
- Preserve the current supported LTS major unless the user explicitly requests a runtime-major change; any proposed replacement must also be an officially supported LTS release. The current baselines are Java/Temurin 25 and Node.js 24.
- Before proposing an update, independently confirm the target's LTS designation and support status in the vendor's official release/support documentation: [Eclipse Temurin](https://adoptium.net/support) and [Node.js](https://nodejs.org/en/about/previous-releases). Record the evidence in the PR; do not infer LTS status from version numbering or a dependency bot proposal.
- Apply this rule consistently to local development, build toolchains, CI, and container build/runtime images. Keep Renovate runtime constraints aligned with the approved LTS baseline; changing those constraints requires the same explicit request and official LTS check.
- Do not invent an LTS requirement for Rust, TypeScript, or other languages/tools that do not offer an official LTS release line. Follow their supported stable-release policy instead.

## Coding Style & Naming Conventions

Backend targets Java 25 and uses Spotless with the Eclipse formatter, Checkstyle, and SpotBugs. Keep packages under `com.example.app`; use clear layer names such as `controller`, `service`, `repository`, and `model.dto`. Test classes should end in `Test`.

Frontend uses TypeScript 7 (`tsc` from the `@typescript/native` alias; `typescript` is aliased to `@typescript/typescript6` for tools that need the TS API, such as StrykerJS), React 19, oxlint with type-aware rules (`.oxlintrc.json`), and pnpm. Use PascalCase for React components and pages, camelCase for functions and hooks, and keep shadcn-style UI primitives in `src/components/ui`.

## Testing Guidelines

Backend tests use JUnit 5, Spring Boot Test, Mockito, Testcontainers, and ArchUnit. JaCoCo branch coverage is enforced at 100% except configured exclusions, so add tests with behavior changes. Frontend tests use Vitest, Testing Library, MSW, and Playwright. Prefer colocated `__tests__` coverage for components, hooks, API clients, and schemas.

## Commit & Pull Request Guidelines

Recent history uses concise imperative subjects, often with Conventional Commit prefixes such as `ci:` or `fix(deps):`. Keep commits scoped. Pull requests should include a summary, linked issue when relevant, test commands run, and screenshots for UI changes.

## Security & Configuration Tips

Do not commit secrets. Backend dev defaults are in `backend/README.md`; production values must override database credentials, session settings, and password pepper. Dependency vulnerabilities are scanned by the `Security scan` workflow (OSV-Scanner over the lockfiles); run `osv-scanner scan source -r .` to check locally. Production container images are scanned weekly by the `Container scan` workflow (`osv-scanner scan image`).
