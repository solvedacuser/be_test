# Backend Implementation Prompt Roadmap

이 문서는 `API_SPEC.md`와 `API_SPEC_EN.md`를 바탕으로 백엔드를 단계별로 구현하기 위한 프롬프트 단위 로드맵이다.

목표는 한 번에 전체 백엔드를 만들기보다, 에이전트가 각 프롬프트를 순서대로 수행하면서 검증 가능한 단위로 구현하는 것이다.

## 전제

| 항목 | 기준 |
| --- | --- |
| Backend framework | Spring Boot |
| Security | Spring Security + JWT |
| Validation | Spring Validation |
| API docs | Swagger / OpenAPI |
| Response format | `success`, `message`, `data`, `code`, `errors` 구조 유지 |
| Auth model | Access Token은 Body/Header, Refresh Token은 HttpOnly Cookie |
| Base URL | `/api` |

명세 기준 파일:

- `backend/API_SPEC.md`
- `backend/API_SPEC_EN.md`

## 구현 원칙

- `API_SPEC.md`와 `API_SPEC_EN.md`에 명시된 API 구현에 필요한 코드만 작성한다.
- 명세에 없는 기능, 엔드포인트, 화면, 관리자 기능, 불필요한 추상화는 추가하지 않는다.
- API 응답 형식은 명세와 다르게 바꾸지 않는다.
- 인증이 필요한 API는 Spring Security Filter에서 Access Token을 검증한다.
- Refresh Token은 JavaScript에서 접근할 수 없도록 HttpOnly Cookie로만 전달한다.
- 비밀번호와 토큰 원문은 로그에 남기지 않는다.
- 각 단계가 끝날 때 테스트 또는 최소한 빌드 검증을 수행한다.
- 프론트엔드 코드는 구현하지 않는다. 백엔드 API와 CORS/Cookie 설정만 구현한다.

## Prompt 0. 프로젝트 상태 파악

### 목적

현재 `backend` 폴더에 Spring Boot 프로젝트가 있는지 확인하고, 없다면 새로 생성할 준비를 한다.

### Agent Prompt

```text
Read backend/API_SPEC.md and backend/API_SPEC_EN.md.

Inspect the backend directory and determine whether a Spring Boot project already exists.

Do not modify files yet.

Return:
1. Current backend project structure.
2. Whether this is an existing project or an empty backend folder.
3. Recommended implementation stack based on the API spec.
4. Any assumptions needed before scaffolding.
```

### 완료 기준

- 기존 프로젝트 여부가 명확하다.
- 사용할 Java, Spring Boot, build tool, database 기준이 정리된다.

## Prompt 1. Spring Boot 프로젝트 스캐폴딩

### 목적

백엔드 기본 프로젝트를 생성한다.

### Agent Prompt

```text
Using backend/API_SPEC.md and backend/API_SPEC_EN.md as the contract, scaffold a Spring Boot backend project inside the backend directory.

Use:
- Spring Boot 3.x
- Java 17 or newer
- Gradle
- Spring Web
- Spring Security
- Spring Validation
- Spring Data JPA
- H2 for local development
- Lombok if appropriate
- springdoc-openapi for Swagger

Create a minimal runnable application.

Do not implement auth logic yet.

After scaffolding:
1. Show the created project structure.
2. Run the build or explain why it cannot be run.
```

### 완료 기준

- `backend` 폴더에서 Spring Boot 앱이 실행 가능한 구조를 가진다.
- 기본 빌드가 성공한다.
- Swagger 의존성, Security, Validation, JPA 의존성이 포함된다.

## Prompt 2. 공통 응답과 에러 모델 구현

### 목적

모든 API가 명세의 공통 응답 형식을 사용하도록 기반 클래스를 만든다.

### Agent Prompt

```text
Implement the common API response and error handling model based on backend/API_SPEC.md.

Required response shapes:

Success:
{
  "success": true,
  "message": "...",
  "data": {}
}

Failure:
{
  "success": false,
  "message": "...",
  "code": "ERROR_CODE",
  "errors": []
}

Implement:
1. ApiResponse<T>
2. ErrorResponse
3. FieldErrorResponse or equivalent validation error item
4. ErrorCode enum
5. Custom business exception
6. Global exception handler using @RestControllerAdvice
7. Validation error handling for @Valid failures

Do not implement auth endpoints yet.

Run tests or at least build the project.
```

### 완료 기준

- 성공/실패 응답 형식이 명세와 일치한다.
- Validation Error 응답에 `field`, `message`가 포함된다.
- 공통 에러 코드가 enum 등으로 중앙 관리된다.

## Prompt 3. User 도메인과 영속성 구현

### 목적

회원가입, 로그인, 내 정보 조회에 필요한 사용자 도메인을 구현한다.

### Agent Prompt

```text
Implement the User domain needed by the API spec.

Required fields:
- userId
- email
- password
- name
- role

Use a secure password encoder for storing passwords.

Implement:
1. User entity
2. Role enum with at least USER
3. UserRepository
4. Basic UserService methods needed later:
   - create user
   - find by email
   - find by id
   - check duplicated email

Do not expose password in any API response.
Do not implement controllers yet unless needed for tests.

Add focused tests for repository/service behavior if the project test setup is ready.
Run build/tests.
```

### 완료 기준

- 사용자 엔티티가 생성된다.
- 이메일 중복 확인이 가능하다.
- 비밀번호는 평문 저장되지 않는다.
- 응답 DTO에 password가 노출되지 않는다.

## Prompt 4. JWT 인프라 구현

### 목적

Access Token과 Refresh Token 생성/검증 기반을 만든다.

### Agent Prompt

```text
Implement JWT infrastructure according to backend/API_SPEC.md.

Requirements:
- Access Token is sent by Authorization header.
- Refresh Token is sent by HttpOnly Cookie.
- Access Token is used for authenticated API requests.
- Refresh Token is used only for Access Token reissue.

Implement:
1. JwtTokenProvider or equivalent
2. Access Token generation
3. Refresh Token generation
4. Token validation
5. Extract user id/email/role from Access Token
6. Token expiration configuration through application properties

Do not log raw tokens.

Add unit tests for token generation and validation.
Run build/tests.
```

### 완료 기준

- Access Token과 Refresh Token을 각각 생성할 수 있다.
- 만료/유효하지 않은 토큰을 구분해 처리할 수 있다.
- 토큰 원문은 로그에 남기지 않는다.

## Prompt 5. Spring Security 설정과 인증 필터 구현

### 목적

인증 필요 API와 공개 API를 분리하고 JWT 인증 필터를 연결한다.

### Agent Prompt

```text
Configure Spring Security based on backend/API_SPEC.md.

Public endpoints:
- POST /api/auth/signup
- POST /api/auth/login
- POST /api/auth/refresh
- Swagger endpoints

Authenticated endpoints:
- POST /api/auth/logout
- GET /api/users/me

Implement:
1. SecurityConfig
2. JWT authentication filter
3. Authentication entry point returning 401 UNAUTHORIZED in the common error format
4. Access denied handler returning 403 FORBIDDEN in the common error format
5. Stateless session policy
6. PasswordEncoder bean

Do not implement business endpoint logic in this prompt unless necessary.

Run build/tests.
```

### 완료 기준

- 공개 API는 토큰 없이 접근 가능하다.
- 보호 API는 Access Token 없이 접근하면 401을 반환한다.
- 권한 부족은 403을 반환한다.
- 401/403 응답 형식이 명세와 일치한다.

## Prompt 6. 회원가입 API 구현

### 목적

`POST /api/auth/signup`을 구현한다.

### Agent Prompt

```text
Implement POST /api/auth/signup based on backend/API_SPEC.md.

Endpoint:
POST /api/auth/signup

Request:
{
  "email": "user@example.com",
  "password": "password123",
  "passwordConfirm": "password123",
  "name": "홍길동"
}

Validation:
- email: required, valid email
- password: required, minimum 8 characters
- passwordConfirm: required, must match password
- name: required, must not be blank

Success:
- HTTP 201 Created
- Return userId, email, name
- Do not return password

Errors:
- 400 VALIDATION_ERROR
- 409 DUPLICATED_EMAIL

Add controller/service tests for success, validation failure, and duplicated email.
Run tests.
```

### 완료 기준

- 회원가입 성공 시 201을 반환한다.
- 중복 이메일이면 409를 반환한다.
- 검증 실패는 400 `VALIDATION_ERROR`를 반환한다.

## Prompt 7. 로그인 API와 Refresh Token Cookie 구현

### 목적

`POST /api/auth/login`을 구현하고 Refresh Token을 HttpOnly Cookie로 내려준다.

### Agent Prompt

```text
Implement POST /api/auth/login based on backend/API_SPEC.md.

Endpoint:
POST /api/auth/login

Request:
{
  "email": "user@example.com",
  "password": "password123"
}

Success:
- HTTP 200 OK
- Response body contains accessToken and user info.
- Refresh Token is sent as an HttpOnly Cookie.

Cookie requirements:
- HttpOnly=true
- Secure=true
- SameSite=None
- Path=/

Errors:
- 400 VALIDATION_ERROR
- 401 INVALID_LOGIN

Important:
- Do not expose password.
- Do not log raw password or raw tokens.

Add tests for successful login and failed login.
Run tests.
```

### 완료 기준

- 로그인 성공 시 Access Token이 Body에 포함된다.
- Refresh Token이 HttpOnly Cookie로 설정된다.
- 로그인 실패 시 401 `INVALID_LOGIN`을 반환한다.

## Prompt 8. Access Token 재발급 API 구현

### 목적

`POST /api/auth/refresh`를 구현한다.

### Agent Prompt

```text
Implement POST /api/auth/refresh based on backend/API_SPEC.md.

Endpoint:
POST /api/auth/refresh

Request:
- No request body.
- Refresh Token is read from Cookie.
- Frontend will send credentials: "include".

Success:
- HTTP 200 OK
- Return a new Access Token in response body.

Errors:
- 401 INVALID_REFRESH_TOKEN
- 401 EXPIRED_REFRESH_TOKEN

Implementation notes:
- Validate the Refresh Token from Cookie.
- Reissue only the Access Token unless the project policy also rotates Refresh Tokens.
- Do not log raw token values.

Add tests:
- valid Refresh Token returns 200 and new Access Token
- invalid Refresh Token returns 401
- expired Refresh Token returns 401 if expiration can be tested

Run tests.
```

### 완료 기준

- 유효한 Refresh Token으로 Access Token 재발급이 가능하다.
- 유효하지 않거나 만료된 Refresh Token은 401을 반환한다.

## Prompt 9. 로그아웃 API 구현

### 목적

`POST /api/auth/logout`을 구현한다.

### Agent Prompt

```text
Implement POST /api/auth/logout based on backend/API_SPEC.md.

Endpoint:
POST /api/auth/logout

Header:
Authorization: Bearer {accessToken}

Success:
- HTTP 200 OK
- Response data is null.
- Server deletes or invalidates the Refresh Token.
- Response should clear the Refresh Token Cookie if cookie-based storage is used.

Errors:
- 401 UNAUTHORIZED when authentication is missing or invalid.

Frontend behavior:
- Frontend will delete the stored Access Token.
- Frontend will redirect to login after logout.

Add tests for successful logout and unauthorized logout.
Run tests.
```

### 완료 기준

- 로그아웃 성공 시 Refresh Token이 무효화된다.
- 쿠키가 제거되거나 만료 처리된다.
- 인증 없이 요청하면 401을 반환한다.

## Prompt 10. 내 정보 조회 API 구현

### 목적

`GET /api/users/me`를 구현한다.

### Agent Prompt

```text
Implement GET /api/users/me based on backend/API_SPEC.md.

Endpoint:
GET /api/users/me

Header:
Authorization: Bearer {accessToken}

Success:
- HTTP 200 OK
- Return userId, email, name, role

Errors:
- 401 UNAUTHORIZED
- 403 FORBIDDEN

Implementation notes:
- Get the current user from the Spring Security authentication context.
- Do not return password or token values.

Add tests:
- successful profile fetch
- request without Access Token returns 401

Run tests.
```

### 완료 기준

- 로그인한 사용자의 정보만 반환한다.
- 비밀번호가 응답에 포함되지 않는다.
- 토큰 없이 요청하면 401을 반환한다.

## Prompt 11. CORS와 Cookie 설정 정리

### 목적

Vercel 프론트와 별도 백엔드 도메인 구성을 지원한다.

### Agent Prompt

```text
Implement CORS and cookie settings based on backend/API_SPEC.md.

Requirements:
- Backend must allow the configured frontend origin.
- Access-Control-Allow-Credentials must be true.
- Do not use "*" as Access-Control-Allow-Origin when credentials are enabled.
- Refresh Token Cookie must use:
  - HttpOnly=true
  - Secure=true
  - SameSite=None
  - Path=/

Make frontend origin configurable through application properties or environment variables.

Add local development settings if needed, but keep production-safe defaults documented.

Run build/tests.
```

### 완료 기준

- CORS origin이 설정값으로 관리된다.
- credentials 요청이 허용된다.
- Cookie 설정이 명세와 일치한다.

## Prompt 12. Swagger 문서화

### 목적

명세에 있는 API를 Swagger에 노출한다.

### Agent Prompt

```text
Add Swagger/OpenAPI documentation for the backend API based on backend/API_SPEC.md.

Document these APIs:
- POST /api/auth/signup
- POST /api/auth/login
- POST /api/auth/refresh
- POST /api/auth/logout
- GET /api/users/me

Include:
- request body schemas
- response schemas
- status codes
- auth requirement for protected endpoints
- common error response shape

Ensure Swagger endpoints are accessible without authentication.

Run build/tests.
```

### 완료 기준

- Swagger UI에서 모든 필수 API를 확인할 수 있다.
- 보호 API는 인증 필요 여부가 표시된다.
- 공통 응답 구조가 문서에 반영된다.

## Prompt 13. Logging 정책 적용

### 목적

필수 로그를 남기되 민감정보는 기록하지 않는다.

### Agent Prompt

```text
Apply logging based on backend/API_SPEC.md.

Log these events:
- sign up success / failure
- login success / failure
- Access Token refresh success / failure
- authentication failure
- authorization failure
- server exception

Security rule:
- Never log raw passwords.
- Never log raw Access Tokens.
- Never log raw Refresh Tokens.

Use structured and useful log messages.

Run build/tests.
```

### 완료 기준

- 필수 이벤트 로그가 존재한다.
- 비밀번호와 토큰 원문이 로그에 남지 않는다.

## Prompt 14. 필수 테스트 완성

### 목적

명세의 필수 테스트 항목을 모두 자동화한다.

### Agent Prompt

```text
Complete the required backend tests based on backend/API_SPEC.md.

Required tests:
1. Successful sign up returns 201.
2. Sign up with duplicated email returns 409.
3. Successful login returns Access Token.
4. Failed login returns 401.
5. Get my profile without Access Token returns 401.
6. Access an API without required permission returns 403.
7. Refresh Access Token with valid Refresh Token returns 200.
8. Use invalid Refresh Token returns 401.
9. Successful logout invalidates Refresh Token.

Use MockMvc or an equivalent Spring testing approach.

Ensure response bodies match the common success/error format.

Run the full test suite.
```

### 완료 기준

- 명세의 필수 테스트가 모두 존재한다.
- 전체 테스트가 성공한다.
- 응답 JSON 구조까지 검증한다.

## Prompt 15. 최종 점검과 API 계약 검증

### 목적

구현 결과가 API 명세와 어긋나지 않는지 최종 점검한다.

### Agent Prompt

```text
Perform a final contract review against backend/API_SPEC.md and backend/API_SPEC_EN.md.

Check:
1. Every endpoint exists with the exact method and path.
2. Public/protected endpoint rules match the spec.
3. Success status codes match the spec.
4. Error status codes and error codes match the spec.
5. Response JSON shape matches the spec.
6. Refresh Token is handled only through HttpOnly Cookie.
7. CORS and credentials settings support Vercel frontend + separate backend domain.
8. Swagger documents all required APIs.
9. Required tests pass.
10. No password or raw token values are logged.

Return:
- Any mismatches found.
- Files changed.
- Test command and result.
```

### 완료 기준

- API 명세와 구현이 일치한다.
- 남은 불일치 또는 미결정 정책이 문서화된다.
- 테스트 결과가 확인된다.

## 권장 구현 순서 요약

| 순서 | Prompt | 산출물 |
| --- | --- | --- |
| 0 | 프로젝트 상태 파악 | 현재 구조 분석 |
| 1 | Spring Boot 스캐폴딩 | 실행 가능한 백엔드 프로젝트 |
| 2 | 공통 응답/에러 | 응답 DTO, 에러 핸들러 |
| 3 | User 도메인 | User Entity, Repository, Service |
| 4 | JWT 인프라 | 토큰 생성/검증 |
| 5 | Security 설정 | 필터, 401/403 처리 |
| 6 | 회원가입 | `/api/auth/signup` |
| 7 | 로그인 | `/api/auth/login`, Refresh Cookie |
| 8 | 토큰 재발급 | `/api/auth/refresh` |
| 9 | 로그아웃 | `/api/auth/logout` |
| 10 | 내 정보 조회 | `/api/users/me` |
| 11 | CORS/Cookie | 배포 환경 설정 |
| 12 | Swagger | API 문서 |
| 13 | Logging | 필수 로그 |
| 14 | 테스트 | 필수 테스트 자동화 |
| 15 | 최종 점검 | 명세 계약 검증 |

## 미리 결정하면 좋은 정책

아래 항목은 명세에 큰 방향은 있지만 구현 세부 정책이 필요하다.

| 항목 | 권장 정책 |
| --- | --- |
| Access Token 만료 시간 | 짧게 설정. 예: 15분 |
| Refresh Token 만료 시간 | 길게 설정. 예: 7일 또는 14일 |
| Refresh Token 저장 방식 | DB 저장 또는 Redis 저장 후 서버에서 무효화 가능하게 관리 |
| Refresh Token rotation | 보안 강화를 원하면 refresh 시 새 Refresh Token도 재발급 |
| Logout with expired Access Token | UX를 위해 Refresh Token Cookie 기준 로그아웃 허용 여부 검토 |
| Local dev cookie Secure | HTTPS가 아니면 local profile에서 `Secure=false` 허용 여부 검토 |
| Frontend origin | 환경변수로 관리. 예: `https://your-app.vercel.app` |

## 최종 완료 체크리스트

- [ ] `POST /api/auth/signup`
- [ ] `POST /api/auth/login`
- [ ] `POST /api/auth/refresh`
- [ ] `POST /api/auth/logout`
- [ ] `GET /api/users/me`
- [ ] JWT Access Token 검증
- [ ] Refresh Token HttpOnly Cookie
- [ ] 공통 성공 응답 형식
- [ ] 공통 실패 응답 형식
- [ ] Validation Error 형식
- [ ] 401 처리
- [ ] 403 처리
- [ ] CORS credentials 설정
- [ ] Swagger 문서화
- [ ] 필수 로그
- [ ] 필수 테스트
