# API Specification

## 1. General API Rules

| Item | Value |
| --- | --- |
| Base URL | `/api` |
| Authentication | JWT Access Token + Refresh Token |
| Access Token delivery | `Authorization: Bearer {accessToken}` |
| Refresh Token delivery | HttpOnly Cookie |
| Response format | JSON |
| API documentation | Swagger UI or a separate API document |
| Deployment | Frontend: Vercel / Backend: separate server |

## 2. Token Policy

### Access Token

| Item | Value |
| --- | --- |
| Storage | Frontend state or storage |
| Delivery method | Authorization header |
| Purpose | Used for authenticated API requests |
| Expiration handling | Request a new Access Token by using the Refresh Token |

### Refresh Token

| Item | Value |
| --- | --- |
| Storage | HttpOnly Cookie |
| Delivery method | Automatically sent by the browser as a Cookie |
| Purpose | Used to reissue Access Tokens |
| Frontend access | Not accessible from JavaScript |
| Logout handling | Deleted or invalidated by the server |

### Cookie Settings

| Option | Value |
| --- | --- |
| HttpOnly | `true` |
| Secure | `true` |
| SameSite | `None` |
| Path | `/` |

If the Vercel frontend and backend server use different domains, cross-site cookie delivery requires:

- `SameSite=None`
- `Secure=true`
- `credentials: "include"` on frontend requests
- `Access-Control-Allow-Credentials: true` on the backend
- A specific frontend origin in `Access-Control-Allow-Origin`, not `*`

## 3. Common Response Format

### Success Response

```json
{
  "success": true,
  "message": "The request was successful.",
  "data": {}
}
```

### Error Response

```json
{
  "success": false,
  "message": "Error message",
  "code": "ERROR_CODE",
  "errors": []
}
```

### Validation Error Example

```json
{
  "success": false,
  "message": "Invalid input.",
  "code": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "email",
      "message": "Invalid email format."
    },
    {
      "field": "password",
      "message": "Password must be at least 8 characters long."
    }
  ]
}
```

## 4. Common Error Codes

| HTTP Status | Code | Meaning | Frontend Handling |
| --- | --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Request validation failed | Show validation messages near the relevant inputs |
| 401 | `UNAUTHORIZED` | Authentication failed or Access Token expired | Try refresh request. If refresh fails, redirect to login |
| 403 | `FORBIDDEN` | Authenticated user does not have permission | Show a forbidden page |
| 409 | `DUPLICATED_EMAIL` | Email already exists | Show duplicate email message |
| 500 | `INTERNAL_SERVER_ERROR` | Server error | Show a common server error message |

## Auth API

### 1. Sign Up

`POST /api/auth/signup`

Creates a new user account.

#### Request Body

```json
{
  "email": "user@example.com",
  "password": "password123",
  "passwordConfirm": "password123",
  "name": "Hong Gil-dong"
}
```

#### Request Validation

| Field | Type | Required | Rule |
| --- | --- | --- | --- |
| email | string | Yes | Must be a valid email format |
| password | string | Yes | Minimum 8 characters |
| passwordConfirm | string | Yes | Must match `password` |
| name | string | Yes | Must not be blank |

#### Success Response

HTTP Status: `201 Created`

```json
{
  "success": true,
  "message": "Sign up completed.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "Hong Gil-dong"
  }
}
```

#### Error Responses

| Status | Code | Message |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Invalid input. |
| 409 | `DUPLICATED_EMAIL` | This email is already in use. |

### 2. Login

`POST /api/auth/login`

Logs in with email and password.

#### Request Body

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### Request Validation

| Field | Type | Required | Rule |
| --- | --- | --- | --- |
| email | string | Yes | Must be a valid email format |
| password | string | Yes | Must not be blank |

#### Success Response

HTTP Status: `200 OK`

```json
{
  "success": true,
  "message": "Login successful.",
  "data": {
    "accessToken": "jwt-access-token",
    "user": {
      "userId": 1,
      "email": "user@example.com",
      "name": "Hong Gil-dong",
      "role": "USER"
    }
  }
}
```

#### Notes

- The Access Token is returned in the response body.
- The Refresh Token is returned as an HttpOnly Cookie.
- After login, the frontend must include the Access Token in the `Authorization` header for authenticated API requests.

#### Error Responses

| Status | Code | Message |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Invalid input. |
| 401 | `INVALID_LOGIN` | Email or password is incorrect. |

### 3. Refresh Access Token

`POST /api/auth/refresh`

Issues a new Access Token by using the Refresh Token.

#### Request

No request body is required.

The Refresh Token is automatically sent as a Cookie.

#### Frontend Request Requirement

```js
credentials: "include"
```

#### Success Response

HTTP Status: `200 OK`

```json
{
  "success": true,
  "message": "Access Token has been refreshed.",
  "data": {
    "accessToken": "new-jwt-access-token"
  }
}
```

#### Error Responses

| Status | Code | Message |
| --- | --- | --- |
| 401 | `INVALID_REFRESH_TOKEN` | Refresh Token is invalid. |
| 401 | `EXPIRED_REFRESH_TOKEN` | Refresh Token has expired. |

#### Frontend Handling

When the Access Token is expired:

1. Request `POST /api/auth/refresh`.
2. If the refresh request succeeds, store the new Access Token.
3. If the refresh request fails, redirect the user to the login page.

### 4. Logout

`POST /api/auth/logout`

Logs out the current user.

#### Header

```http
Authorization: Bearer {accessToken}
```

#### Success Response

HTTP Status: `200 OK`

```json
{
  "success": true,
  "message": "Logged out.",
  "data": null
}
```

#### Notes

- The server must delete or invalidate the Refresh Token.
- The frontend must delete the stored Access Token.
- After logout, redirect the user to the login page.

#### Error Responses

| Status | Code | Message |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | Authentication is required. |

## User API

### 5. Get My Profile

`GET /api/users/me`

Returns the currently logged-in user's information.

#### Header

```http
Authorization: Bearer {accessToken}
```

#### Success Response

HTTP Status: `200 OK`

```json
{
  "success": true,
  "message": "Fetched my profile successfully.",
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "name": "Hong Gil-dong",
    "role": "USER"
  }
}
```

#### Error Responses

| Status | Code | Message |
| --- | --- | --- |
| 401 | `UNAUTHORIZED` | Authentication is required. |
| 403 | `FORBIDDEN` | Access is forbidden. |

## Frontend Implementation Requirements

### 1. Protected Routes

#### Protected Route Targets

| Page | Access Condition |
| --- | --- |
| My Page | Login required |
| Permission-required page | Login and required permission |

#### Flow

1. User opens a protected page.
2. Frontend checks whether an Access Token exists.
3. If the Access Token exists, request the API.
4. If the Access Token is missing or expired, request `POST /api/auth/refresh`.
5. If refresh succeeds, allow access.
6. If refresh fails, redirect to the login page.

### 2. Keep Login State After Page Refresh

If the Access Token is lost after a browser refresh, use this flow:

1. App starts initial loading.
2. Request `POST /api/auth/refresh`.
3. If refresh succeeds, store the new Access Token.
4. Request `GET /api/users/me` to load user information.
5. If refresh fails, treat the user as logged out.

### 3. Handling 401 and 403

| Status | Meaning | Frontend Handling |
| --- | --- | --- |
| 401 | Authentication failed or token expired | Try refresh request. If refresh fails, redirect to login |
| 403 | User is authenticated but lacks permission | Show a forbidden page |

### 4. Sign Up Form Validation

| Field | Validation Rule |
| --- | --- |
| Email | Must be a valid email format |
| Password | Minimum 8 characters |
| Password confirmation | Must match password |
| Required fields | Must not be missing |

## Backend Implementation Requirements

### 1. Authentication and Authorization

- Use Spring Security.
- Validate JWT Access Tokens.
- Reissue Access Tokens using Refresh Tokens.
- APIs that require authentication must be validated in the Security Filter.
- Return `403 FORBIDDEN` when the authenticated user lacks permission.

### 2. Request Validation

Use Spring Validation.

Example:

```java
@NotBlank
@Email
private String email;

@NotBlank
@Size(min = 8)
private String password;
```

### 3. Swagger Documentation

The following APIs must be included in Swagger:

| API | Included |
| --- | --- |
| Sign Up | Yes |
| Login | Yes |
| Refresh Access Token | Yes |
| Logout | Yes |
| Get My Profile | Yes |

### 4. CORS and Cookie Settings

If frontend and backend use different domains, the following settings are required.

#### Backend

```http
Access-Control-Allow-Origin: {frontend-origin}
Access-Control-Allow-Credentials: true
```

#### Frontend

```js
credentials: "include"
```

### 5. Logging

The backend must log these events:

- Sign up success / failure
- Login success / failure
- Access Token refresh success / failure
- Authentication failure
- Authorization failure
- Server exception

Do not log raw passwords or raw tokens.

### 6. Required Tests

| Test Case | Expected Result |
| --- | --- |
| Successful sign up | Returns 201 |
| Sign up with duplicated email | Returns 409 |
| Successful login | Returns Access Token |
| Failed login | Returns 401 |
| Get my profile without Access Token | Returns 401 |
| Access an API without required permission | Returns 403 |
| Refresh Access Token with valid Refresh Token | Returns 200 |
| Use invalid Refresh Token | Returns 401 |
| Successful logout | Refresh Token is invalidated |

## API Summary

| Method | Endpoint | Description | Auth |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | Sign up | No |
| POST | `/api/auth/login` | Login | No |
| POST | `/api/auth/refresh` | Refresh Access Token | Refresh Token Cookie |
| POST | `/api/auth/logout` | Logout | Yes |
| GET | `/api/users/me` | Get my profile | Yes |
