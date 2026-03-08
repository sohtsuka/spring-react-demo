# Architecture Consistency Checker - Memory

## Project: spring-react-demo (fullstack monorepo)
- Backend path: ./backend
- Frontend path: ./frontend

### Backend: Identified Architecture Pattern
Layered Architecture (Controller -> Service -> Repository -> Domain)
Confidence: High

### Backend Package Structure
```
com.example.app
├── config/    # FlywayConfig, MyBatisConfig, SecurityConfig, WebConfig
├── controller/  # AuthController, UserController
├── exception/  # AppException, ErrorCode, GlobalExceptionHandler
├── model/
│   ├── dto/   # Java records (ApiResponse, ErrorResponse, PagedResponse,
│   │            PaginationMeta, CreateUserRequest, UpdateUserRequest,
│   │            UserResponse, LoginRequest)
│   ├── entity/ # User (plain POJO, no JPA — intentional, MyBatis)
│   └── enums/ # UserRole
├── repository/ # UserRepository (@Mapper interface)
├── security/  # CustomUserDetails, CustomUserDetailsService, PepperPasswordEncoder
├── service/   # UserService (interface)
│   └── impl/  # UserServiceImpl
└── util/      # PasswordHasher (CLI), RateLimitInterceptor, RequestLoggingInterceptor
```

### Backend Established Conventions
- DTOs: Java records; naming uses Request/Response suffix (not *DTO)
- Entity: plain POJO, no JPA; intentional for MyBatis
- Service: interface in service/, impl in service/impl/ — DI inversion correct
- Repository: MyBatis @Mapper; SQL XML in resources/mapper/
- @Transactional: class-level on @Service, method-level readOnly override — correct
- Controllers depend on UserService interface, not impl — correct
- Entities NOT Spring-managed beans — correct
- Config classes in config/ — correct
- @PreAuthorize at controller method level for RBAC

### Backend Known Issues (comprehensive audit 2026-03-08)
CRITICAL:
- CustomUserDetailsService in security/ directly injects UserRepository,
  bypassing the Service layer. Should go through UserService.
- UserResponse.from(User) static factory in DTO creates domain-to-DTO mapping
  inside the DTO itself. Should use a dedicated Mapper class.
- AuthController performs manual session fixation (invalidate old, create new)
  that duplicates Spring Security's sessionFixation().newSession() already in
  SecurityConfig. Both fire on login — redundant and inconsistent.

WARNINGS:
- util/ mixes interceptors (infrastructure) with PasswordHasher (CLI tool).
- ErrorCode imports org.springframework.http.HttpStatus — Spring dep in cross-cutting.
- SecurityConfig has inner class CsrfCookieFilter — should be top-level in security/.
- ObjectMapper: new ObjectMapper() in SecurityConfig and RateLimitInterceptor,
  not Spring-managed beans.
- MyBatis config duplicated: MyBatisConfig.java and application.yml both set
  mapUnderscoreToCamelCase, defaultFetchSize, defaultStatementTimeout.
- UserService conflates CRUD with auth-concern methods (recordLoginFailure,
  resetLoginAttempts). These belong in a separate AuthService.
- FlywayConfig calls flyway.repair() unconditionally on every startup —
  repair is a maintenance op, not suitable for production startup path.
- UpdateUserRequest has no @NotBlank/@NotNull constraints; service applies
  partial-update logic not reflected in DTO's validation contract.

SUGGESTIONS:
- No Mapper abstraction; entity-to-DTO via static factory on DTO record.
- RateLimitInterceptor: in-memory ConcurrentHashMap buckets — not distributed.
- LoginPage checks raw HTTP status codes (401/423) instead of error body `code`.

### Frontend: Identified Architecture Pattern
Feature-based layered SPA (api / hooks / pages / components / lib / schemas / types)
Confidence: High

### Frontend Package Structure
```
src/
├── api/        # authApi, usersApi (axios wrappers, object literal pattern)
├── components/
│   ├── common/ # ProtectedRoute, RoleProtectedRoute, Layout
│   └── ui/     # shadcn/ui generated components
├── hooks/      # useAuth, useToast
├── lib/        # axios instance, queryClient, utils (cn)
├── pages/      # page-level components (*Page suffix)
│   ├── admin/  # AdminPage, UserFormDialog
│   └── manager/ # ManagerPage
├── schemas/    # Zod schemas with inferred types exported alongside
├── types/      # index.ts — all shared TypeScript types
└── test/       # MSW handlers, renderWithProviders, mockUser
```

### Frontend Established Conventions
- Named exports for all components and hooks
- Pages use Page suffix
- Custom hooks use use prefix
- API layer: object literal pattern (authApi.login())
- TanStack Query for server state; React Hook Form + Zod for forms
- Types centralised in types/index.ts

### Frontend Known Issues (comprehensive audit 2026-03-08)
WARNINGS:
- AdminPage and ManagerPage share queryKey ['users'] and duplicate table JSX.
  No shared UserTable component or useUsers hook.
- UserFormDialog contains two full form instances (create + update) in one
  component, making it very large; could be split into focused dialogs.
- AuthUser type is a subset of User; fragile since some pages use useAuth()
  but expect full User fields (createdAt/updatedAt). Currently safe.
- queryClient 401 handler uses window.location.href (hard navigation) vs
  React Router navigate() used elsewhere — intentional for cache clearing.
- LoginPage imports axios directly for isAxiosError — bypasses @/lib/axios layer.
- Layout navItems.roles is typed string[] not Role[] from types/index.ts.
- updateUserSchema makes all fields required but backend UpdateUserRequest
  allows optional partial updates — frontend/backend contract mismatch.

### Spring Boot / Java Specifics
- Spring Boot 4, Java 25; MyBatis, Flyway, PostgreSQL
- Session-based auth; Argon2id + app-salt + pepper (PepperPasswordEncoder)
- CSRF: CookieCsrfTokenRepository; login endpoint excluded from CSRF
- Rate limiting: Bucket4j HandlerInterceptor, login endpoint only
