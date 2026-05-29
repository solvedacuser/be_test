# API 명세서

## 1. API 기본 규칙

| 항목 | 내용 |
| --- | --- |
| Base URL | `/api` |
| 인증 방식 | JWT Access Token + Refresh Token |
| Access Token 전달 | `Authorization: Bearer {accessToken}` |
| Refresh Token 전달 | HttpOnly Cookie |
| 응답 형식 | JSON |
| API 문서화 | Swagger UI 또는 별도 문서 |
| 배포 환경 | Frontend: Vercel / Backend: 별도 서버 |

## 2. 인증 토큰 정책

### Access Token

| 항목 | 내용 |
| --- | --- |
| 저장 위치 | 프론트 상태 또는 스토리지 |
| 전달 방식 | Authorization Header |
| 사용 목적 | 인증이 필요한 API 요청 |
| 만료 시 처리 | Refresh Token으로 재발급 요청 |

### Refresh Token

| 항목 | 내용 |
| --- | --- |
| 저장 위치 | HttpOnly Cookie |
| 전달 방식 | Cookie 자동 전송 |
| 사용 목적 | Access Token 재발급 |
| 프론트 접근 여부 | JavaScript에서 접근 불가 |
| 로그아웃 시 처리 | 서버에서 삭제 또는 무효화 |

### Cookie 설정 기준

| 옵션 | 값 |
| --- | --- |
| HttpOnly | `true` |
| Secure | `true` |
| SameSite | `None` |
| Path | `/` |

Vercel 프론트와 백엔드 서버 도메인이 다르면 쿠키 전송을 위해 `SameSite=None`, `Secure=true`, `credentials: include` 설정이 필요하다.

## 3. 공통 응답 형식

### 성공 응답

```json
{
  "success": true,
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

### 실패 응답

```json
{
  "success": false,
  "message": "에러 메시지",
  "code": "ERROR_CODE",
  "errors": []
}
```

### Validation Error 예시

```json
{
  "success": false,
  "message": "입력값이 올바르지 않습니다.",
  "code": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "email",
      "message": "이메일 형식이 올바르지 않습니다."
    },
    {
      "field": "password",
      "message": "비밀번호는 최소 8자 이상이어야 합니다."
    }
  ]
}
```

## 4. 공통 에러 코드

| HTTP Status | Code | 의미 | 프론트 처리 |
| --- | --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 요청 값 검증 실패 | 입력값 에러 메시지 표시 |
| 401 | `UNAUTHORIZED` | 인증 실패 또는 Access Token 만료 | Refresh 요청 후 실패 시 로그인 페이지 이동 |
| 403 | `FORBIDDEN` | 권한 부족 | 권한 없음 페이지 표시 |
| 409 | `DUPLICATED_EMAIL` | 이메일 중복 | 중복 이메일 메시지 표시 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 오류 | 공통 서버 오류 메시지 표시 |

## Auth API

### 1. 회원가입

`POST /api/auth/signup`

회원가입을 진행한다.

#### Request

```json
{
  "email": "user@example.com",
  "password": "password123",
  "passwordConfirm": "password123",
  "name": "홍길동"
}
```

#### Request Validation

| 필드 | 타입 | 필수 | 조건 |
| --- | --- | --- | --- |
| email | string | O | 이메일 형식 |
| password | string | O | 최소 8자 이상 |
| passwordConfirm | string | O | password와 일치 |
| name | string | O | 빈 값 불가 |

#### Success Response

HTTP Status: `201 Created`

```json
{
  "success": true,
  "message": "회원가입이 완료되었습니다.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동"
  }
}
```

#### Error Response

| Status | Code | Message |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 입력값이 올바르지 않습니다. |
| 409 | `DUPLICATED_EMAIL` | 이미 사용 중인 이메일입니다. |

### 2. 로그인

`POST /api/auth/login`

이메일과 비밀번호로 로그인한다.

#### Request

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### Request Validation

| 필드 | 타입 | 필수 | 조건 |
| --- | --- | --- | --- |
| email | string | O | 이메일 형식 |
| password | string | O | 빈 값 불가 |

#### Success Response

HTTP Status: `200 OK`

```json
{
  "success": true,
  "message": "로그인에 성공했습니다.",
  "data": {
    "accessToken": "jwt-access-token",
    "user": {
      "userId": 1,
      "email": "user@example.com",
      "name": "홍길동",
      "role": "USER"
    }
  }
}
```

#### 비고

- Access Token은 응답 Body로 전달한다.
- Refresh Token은 HttpOnly Cookie로 전달한다.
- 프론트는 이후 인증 API 요청 시 Access Token을 Authorization Header에 포함한다.

#### Error Response

| Status | Code | Message |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | 입력값이 올바르지 않습니다. |
| 401 | `INVALID_LOGIN` | 이메일 또는 비밀번호가 올바르지 않습니다. |

### 3. Access Token 재발급

`POST /api/auth/refresh`

Refresh Token을 이용해 Access Token을 재발급한다.

#### Request

별도 Request Body 없음.

Refresh Token은 Cookie로 자동 전달된다.

#### Frontend 요청 조건

```js
credentials: "include"
```

#### Success Response

HTTP Status: `200 OK`

```json
{
  "success": true,
  "message": "Access Token이 재발급되었습니다.",
  "data": {
    "accessToken": "new-jwt-access-token"
  }
}
```

#### Error Response

| Status | Code | Message |
| --- | --- | --- |
| 401 | `INVALID_REFRESH_TOKEN` | Refresh Token이 유효하지 않습니다. |
| 401 | `EXPIRED_REFRESH_TOKEN` | Refresh Token이 만료되었습니다. |

#### 프론트 처리 기준

Access Token 만료
-> `/api/auth/refresh` 요청
-> 성공 시 새 Access Token 저장
-> 실패 시 로그인 페이지 이동

### 4. 로그아웃

`POST /api/auth/logout`

로그아웃을 진행한다.

#### Header

```http
Authorization: Bearer {accessToken}
```

#### Success Response

HTTP Status: `200 OK`

```json
{
  "success": true,
  "message": "로그아웃되었습니다.",
  "data": null
}
```

#### 비고

- 서버는 Refresh Token을 삭제 또는 무효화한다.
- 프론트는 저장된 Access Token을 삭제한다.
- 로그아웃 후 로그인 페이지로 이동한다.

#### Error Response

| Status | Code | Message |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 인증이 필요합니다. |

## User API

### 5. 내 정보 조회

`GET /api/users/me`

로그인한 사용자의 정보를 조회한다.

#### Header

```http
Authorization: Bearer {accessToken}
```

#### Success Response

HTTP Status: `200 OK`

```json
{
  "success": true,
  "message": "내 정보 조회에 성공했습니다.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER"
  }
}
```

#### Error Response

| Status | Code | Message |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | 인증이 필요합니다. |
| 403 | `FORBIDDEN` | 접근 권한이 없습니다. |

## Frontend 필수 구현 기준

### 1. 보호 라우트

#### 보호 라우트 대상

| 페이지 | 접근 조건 |
| --- | --- |
| 마이페이지 | 로그인 필요 |
| 권한 필요 페이지 | 로그인 + 권한 필요 |

#### 처리 흐름

페이지 접근
-> Access Token 확인
-> Access Token 있음: API 요청
-> Access Token 없음 또는 만료: `/api/auth/refresh` 요청
-> Refresh 성공: 접근 허용
-> Refresh 실패: 로그인 페이지 이동

### 2. 새로고침 시 로그인 유지

새로고침 시 Access Token이 사라진 경우 다음 순서로 처리한다.

앱 초기 로딩
-> `/api/auth/refresh` 요청
-> 성공 시 Access Token 재저장
-> `/api/users/me` 요청으로 사용자 정보 조회
-> 실패 시 비로그인 상태 처리

### 3. 401 / 403 처리

| 상태 | 의미 | 프론트 처리 |
| --- | --- | --- |
| 401 | 인증 실패 또는 토큰 만료 | Refresh 요청 후 실패 시 로그인 페이지 이동 |
| 403 | 인증은 되었지만 권한 부족 | 권한 없음 페이지 표시 |

### 4. 회원가입 화면 검증

| 항목 | 검증 조건 |
| --- | --- |
| 이메일 | 이메일 형식 확인 |
| 비밀번호 | 최소 8자 이상 |
| 비밀번호 확인 | 비밀번호와 일치 여부 확인 |
| 필수 입력값 | 누락 여부 확인 |

## Backend 필수 구현 기준

### 1. 인증 / 인가

- Spring Security 사용
- JWT Access Token 검증
- Refresh Token 기반 Access Token 재발급
- 인증 필요 API는 Security Filter에서 검증
- 권한 부족 시 403 반환

### 2. 요청 값 검증

Spring Validation 사용.

예시:

```java
@NotBlank
@Email
private String email;

@NotBlank
@Size(min = 8)
private String password;
```

### 3. Swagger 문서화

Swagger에 포함할 API:

| API | 포함 여부 |
| --- | --- |
| 회원가입 | O |
| 로그인 | O |
| Access Token 재발급 | O |
| 로그아웃 | O |
| 내 정보 조회 | O |

### 4. CORS / Cookie 설정

프론트와 백엔드 도메인이 다를 경우 다음 설정이 필요하다.

#### Backend

```http
Access-Control-Allow-Origin: 프론트엔드 도메인
Access-Control-Allow-Credentials: true
```

#### Frontend

```js
credentials: "include"
```

### 5. Logging

필수 로그 대상:

- 회원가입 성공 / 실패
- 로그인 성공 / 실패
- Access Token 재발급 성공 / 실패
- 인증 실패
- 권한 부족
- 서버 예외

단, 비밀번호와 토큰 원문은 로그에 남기지 않는다.

### 6. 테스트 코드

필수 테스트 항목:

| 테스트 | 기대 결과 |
| --- | --- |
| 회원가입 성공 | 201 반환 |
| 이메일 중복 회원가입 실패 | 409 반환 |
| 로그인 성공 | Access Token 반환 |
| 로그인 실패 | 401 반환 |
| Access Token 없이 내 정보 조회 | 401 반환 |
| 권한 부족 API 접근 | 403 반환 |
| Refresh Token으로 Access Token 재발급 | 200 반환 |
| 유효하지 않은 Refresh Token 사용 | 401 반환 |
| 로그아웃 성공 | Refresh Token 무효화 |

## API 목록 요약

| Method | Endpoint | 설명 | 인증 |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | 회원가입 | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/refresh` | Access Token 재발급 | Refresh Token Cookie |
| POST | `/api/auth/logout` | 로그아웃 | O |
| GET | `/api/users/me` | 내 정보 조회 | O |
