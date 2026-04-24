You are a senior spring boot engineer working on a Spring Boot 3.2 REST API called
RideList. Four gaps have been identified during
frontend integration analysis. Fix all four.

Read the existing codebase thoroughly before making
any changes. Do not break existing functionality.

---

GAP 1 — Registration accountType + auto-upgrade flow

CURRENT BEHAVIOR
RegisterRequest does not accept accountType. All new
users default to INDIVIDUAL. Auto-upgrade to DEALER
occurs when a user posts their first listing.

REQUIRED CHANGES

1. Add optional accountType field to RegisterRequest:

   @JsonProperty("accountType")
   private AccountType accountType;

  - Field is optional — if null, default to INDIVIDUAL
  - If provided, set the user's AccountType to that
    value on registration
  - Valid values: INDIVIDUAL, DEALER
  - Invalid values should return 400 Bad Request

2. Keep the existing auto-upgrade behavior intact:
  - If a user registered as INDIVIDUAL and posts
    their first listing, upgrade to DEALER as before
  - If a user registered as DEALER, no change needed

3. Update AuthService.register() accordingly:

   AccountType type = request.getAccountType() != null
   ? request.getAccountType()
   : AccountType.INDIVIDUAL;
   user.setAccountType(type);

4. Add accountType to AuthResponse and UserResponse
   so the frontend receives it after login/register.
   Confirm it is already present — if not, add it.

5. Add a migration if the users table needs any
   schema change (it likely does not since
   account_type column already exists).

---

GAP 2 — Profile update endpoint

Add the following endpoints to a new or existing
UserController at base path /api/v1/account.

GET /api/v1/account/me
- Returns the currently authenticated user's profile
- Auth: required (any authenticated user)
- Response: UserResponse (id, firstName, lastName,
  email, accountType, role, state, createdAt)

PUT /api/v1/account/me
- Updates the authenticated user's profile
- Auth: required
- Request body: UpdateProfileRequest
  {
  "firstName": "string (optional)",
  "lastName": "string (optional)",  
  "stateId": "UUID (optional)"
  }
- Only updates fields that are provided (partial
  update — ignore null fields)
- Response: updated UserResponse

PUT /api/v1/account/me/password
- Changes the authenticated user's password
- Auth: required
- Request body: ChangePasswordRequest
  {
  "currentPassword": "string (required)",
  "newPassword": "string (required, min 8 chars)",
  "confirmPassword": "string (required)"
  }
- Validate currentPassword matches stored BCrypt hash
- Validate newPassword and confirmPassword match
- Return 400 if currentPassword is wrong
- Return 400 if passwords do not match
- Response: ApiResponse.success("Password updated
  successfully")

DELETE /api/v1/account/me
- Permanently deletes the authenticated user's account
- Auth: required
- Soft or hard delete — match existing pattern in
  the codebase
- Also deletes or marks as DELETED all listings
  owned by this user
- Response: ApiResponse.success("Account deleted")

Create UpdateProfileRequest, ChangePasswordRequest
DTOs with Jakarta validation annotations.
Create UserService if it does not exist with:
- getProfile(UUID userId)
- updateProfile(UUID userId, UpdateProfileRequest)
- changePassword(UUID userId, ChangePasswordRequest)
- deleteAccount(UUID userId)

Add these endpoints to SecurityConfig as authenticated
(not public, not admin-only).

---

GAP 3 — Admin categorization endpoints

The admin panel manages Make → VehicleModel → ModelYear
hierarchy. These CRUD endpoints are missing entirely.

Add to a new AdminCategorizationController at base
path /api/v1/admin/categorization.
Require ROLE_ADMIN on all endpoints.
Follow the exact same pattern as the existing
AdminLocationController.

MAKE ENDPOINTS
POST   /makes           — Create make
PUT    /makes/{id}      — Update make name
DELETE /makes/{id}      — Delete make (cascade to
models and years)
GET    /makes           — Get all makes

VEHICLE MODEL ENDPOINTS
POST   /models          — Create model (requires makeId)
PUT    /models/{id}     — Update model name
DELETE /models/{id}     — Delete model (cascade to years)
GET    /makes/{makeId}/models — Get models for a make

MODEL YEAR ENDPOINTS
POST   /years           — Create year (requires modelId)
PUT    /years/{id}      — Update year name
DELETE /years/{id}      — Delete year
GET    /models/{modelId}/years — Get years for a model

Request DTOs (follow same pattern as location DTOs):
- CreateMakeRequest        — { name }
- CreateVehicleModelRequest — { name, makeId }
- CreateModelYearRequest   — { name, modelYearId }
- UpdateCategorizationRequest — { name }

Validation:
- Slug uniqueness enforced (DuplicateResourceException)
- Parent entity must exist (ResourceNotFoundException)
- Delete cascade warns if listings reference this
  make/model/year — do not block delete, just cascade

Cache invalidation:
- All write operations must call cache.evictAll()
  exactly as LocationService does
- This ensures lookup endpoints reflect changes
  immediately

Create CategorizationService if it does not exist.
If it already exists, extend it with these methods.

---

GAP 4 — Token refresh endpoint

Add a token refresh endpoint to AuthController.

POST /api/v1/auth/refresh
- Public endpoint (no Bearer token required)
- Request body: RefreshTokenRequest
  {
  "refreshToken": "string (required)"
  }
- Validate the refreshToken:
  - Must be a valid JWT signed with the same secret
  - Must not be expired
  - Extract userId from claims
  - Load user from database to confirm account exists
- On success: generate a new accessToken and return:
  {
  "accessToken": "string",
  "tokenType": "Bearer",
  "expiresIn": 86400
  }
- On failure:
  - Invalid token: 401 Unauthorized
  - Expired token: 401 Unauthorized with message
    "Refresh token expired. Please log in again."
  - User not found: 401 Unauthorized

Generate refresh token in AuthService.register()
and AuthService.login() if not already doing so.
Refresh token should have a longer expiry than
access token — 7 days recommended.

Add RefreshTokenRequest DTO with validation.
Add the endpoint to SecurityConfig as public
(same as /auth/register and /auth/login).

---

AFTER ALL CHANGES

1. Add a Flyway migration if any schema changes
   were made (profile fields, token fields, etc.)

2. Confirm all four new/updated endpoint groups
   are covered by the existing GlobalExceptionHandler

3. Add the new endpoints to SecurityConfig in the
   correct access tier:
  - /api/v1/auth/refresh → public
  - /api/v1/account/me → authenticated
  - /api/v1/account/me/password → authenticated
  - /api/v1/account/me (DELETE) → authenticated
  - /api/v1/admin/categorization/** → ROLE_ADMIN

4. Add OpenAPI/Swagger annotations to all new
   endpoints so the spec stays current

5. After completing all changes, provide a summary:
  - Files created
  - Files modified
  - Migrations added
  - Any decisions made where the spec was ambiguous


### Update context
Update the CLAUDE.md file of this project with this newly added context/functionalities