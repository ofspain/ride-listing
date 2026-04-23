# RideList - Marketplace Platform

## Project Overview

RideList is a **marketplace for motorcycles, tricycles, bicycles, spare parts, and accessories** targeting the Nigerian market. It's a Spring Boot 3.2 REST API with JWT authentication and AWS S3 for image storage.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Framework | Spring Boot 3.2.4, Java 21 |
| Database | PostgreSQL with Flyway migrations |
| Auth | JWT (jjwt 0.12.5), Spring Security |
| Storage | AWS S3 (SDK v2) for images |
| Mapping | MapStruct 1.5.5 with Lombok binding |
| Logging | Logback with Logstash JSON encoder |
| Monitoring | Spring Boot Actuator |
| Build | Maven |

## Project Structure

```
src/main/java/com/ridelist/
├── cache/           # In-memory caching components (InMemoryCache)
├── config/          # Configuration classes (Security, S3, JWT properties)
├── controller/      # REST controllers
├── dto/
│   ├── mapper/      # MapStruct mappers
│   ├── request/     # Inbound DTOs with validation
│   └── response/    # Outbound DTOs
├── exception/       # Global exception handling
├── filter/          # Servlet filters (CorrelationIdFilter)
├── model/           # JPA entities and enums
├── repository/      # Spring Data JPA repositories
├── security/        # JWT filter, token provider, UserPrincipal
├── service/         # Business logic
└── util/            # Utilities (@CurrentUser, SlugUtil, LogContext)
```

## Domain Model

### Core Entities

- **User** - accounts with `AccountType` (INDIVIDUAL/DEALER), state, role, soft delete support
- **Listing** - single table for vehicles and parts, discriminated by `ListingType`
- **ListingImage** - S3-backed images with display order
- **ContactRequest** - buyer inquiries (supports both authenticated and anonymous)
- **Favorite** - user's saved listings

### Location Entities (3-Level Hierarchy)

- **State** - Nigerian states (top level)
- **Axis** - platform-defined partitions within a state
- **Area** - platform-defined partitions within an axis

**Relationships:**
```
State (1) ←→ (N) Axis (1) ←→ (N) Area
```

**Fields (common to all):**
- `id` (UUID, PK)
- `name` (String)
- `slug` (String, unique, indexed)
- `createdAt` (LocalDateTime)

**Constraints:**
- Slugs are lowercase, hyphen-separated, URL-safe
- Axis must belong to the selected state
- Area must belong to the selected axis
- Validation enforced at service layer

### Vehicle Categorization Entities (3-Level Hierarchy)

- **Make** - Vehicle manufacturer (e.g., TVS, Honda, Bajaj)
- **VehicleModel** - Specific model of a make (e.g., Apache, Activa)
- **ModelYear** - Year/edition of a model (e.g., 2024, 2023)

**Relationships:**
```
Make (1) ←→ (N) VehicleModel (1) ←→ (N) ModelYear
```

**Fields (common to all):**
- `id` (UUID, PK)
- `name` (String)
- `slug` (String, unique, indexed)
- `createdAt` (LocalDateTime)

**Constraints:**
- Slugs are lowercase, hyphen-separated, URL-safe (generated via `SlugUtil.toSlug()`)
- VehicleModel must belong to the selected make
- ModelYear must belong to the selected vehicle model
- Validation enforced at service layer

**Listing Integration:**
- Listing entity has optional `make`, `vehicleModel`, `modelYear` relationships (ManyToOne)
- Only applicable to vehicle listings (`listingType = VEHICLE`)
- Part listings do not use categorization hierarchy

### Dynamic Attributes System

Allows admins to define filterable attributes (e.g., engine type, fuel type, color), sellers to provide values, and users to filter/search using them.

**Entities:**

- **AttributeDefinition** - Admin-defined attributes
  - `id` (UUID, PK)
  - `name` (String) - e.g., "Engine Type"
  - `slug` (String, unique, indexed) - e.g., "engine-type"
  - `listingType` (Enum: VEHICLE, PART) - applies to which listing type
  - `filterable` (Boolean) - usable in search filters
  - `required` (Boolean) - must be provided when creating listing
  - `active` (Boolean) - whether attribute is currently active
  - `createdAt` (LocalDateTime)

- **ListingAttributeValue** - Stores attribute values per listing
  - `id` (UUID, PK)
  - `listing` (ManyToOne → Listing)
  - `attribute` (ManyToOne → AttributeDefinition)
  - `value` (String) - free text value
  - `createdAt` (LocalDateTime)

**Constraints:**
- Unique (listing_id, attribute_id) - one value per attribute per listing
- Attribute must be active to be used
- Attribute must match listing's listingType
- Required attributes enforced at service layer

**Listing Integration:**
- Listing entity has `attributes` relationship (OneToMany → ListingAttributeValue)
- Attributes passed in `CreateListingRequest` and `UpdateListingRequest` as:
  ```json
  {
    "attributes": [
      { "attributeId": "uuid", "value": "150cc" },
      { "attributeId": "uuid", "value": "Petrol" }
    ]
  }
  ```

### Key Enums

- `ListingType`: VEHICLE, PART
- `VehicleType`: MOTORCYCLE, TRICYCLE, BICYCLE
- `ListingCategory`: MOTORCYCLE, TRICYCLE, BICYCLE, SPARE_PART, ACCESSORY
- `ListingStatus`: DRAFT, PUBLISHED, ACTIVE, SOLD, EXPIRED, DELETED
- `ListingCondition`: NEW, LIKE_NEW, GOOD, FAIR, POOR
- `AccountType`: INDIVIDUAL, DEALER
- `Role`: USER, ADMIN

## API Design Patterns

### Response Wrapper
All responses use `ApiResponse<T>`:
```java
ApiResponse.success(data)           // Success with data
ApiResponse.success(message, data)  // Success with message
ApiResponse.error(message)          // Error
```

### Pagination
Use `PagedResponse<T>` for paginated endpoints.

### Validation
Jakarta validation on request DTOs. Errors handled by `GlobalExceptionHandler`.

## Security

### JWT Configuration

JWT tokens in Authorization header: `Bearer <token>`

**Components:**
- `JwtTokenProvider` - Generates and validates JWT tokens
- `JwtAuthenticationFilter` - Extracts and validates JWT from requests
- `JwtAuthenticationEntryPoint` - Handles unauthorized access
- `CustomUserDetailsService` - Loads user by ID for JWT validation

### Access Rules

| Endpoint | Access |
|----------|--------|
| `/api/v1/auth/**` | Public |
| `GET /api/v1/listings/**` | Public |
| `GET /api/v1/attributes/**` | Public |
| `GET /api/v1/lookup/**` | Public |
| `POST /api/v1/listings/*/inquire` | Public |
| `/api/v1/admin/**` | ROLE_ADMIN only |
| `/api/v1/account/**` | Authenticated |
| All other endpoints | Authenticated |

### Password Encoding

BCrypt password encoder configured via `SecurityConfig`.

### Getting Current User

Use `@CurrentUser UserPrincipal` annotation in controllers:
```java
@GetMapping("/me")
public ApiResponse<UserResponse> getMe(@CurrentUser UserPrincipal principal) {
    UUID userId = principal.getId();
}
```

## Database

### Migrations
Located in `src/main/resources/db/migration/`:
- V1: Initial schema (users, listings, listing_images, contact_requests)
- V2: Account fields (state, account_type on users)
- V3: Listing type fields (listing_type, vehicle_type, part fields)
- V4: Guest inquiry support (nullable buyer_id, sender_name/phone)
- V5: Favorites table
- V6: Location hierarchy (states, axes, areas tables + listing location columns)
- V7: Vehicle categorization (makes, vehicle_models, model_years tables + listing categorization columns)
- V8: Dynamic attributes (attribute_definitions, listing_attribute_values tables)
- V9: Soft delete support (deleted_at column on users table)

### Connection
Configured via environment variables:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`

## AWS S3 Configuration

Uses AWS SDK v2 for S3 integration.

### Configuration Classes

- `AwsS3Properties` - Binds to `aws.*` properties
- `AwsS3Config` - Creates `S3Client` and `S3Presigner` beans

### Environment Variables

```yaml
aws:
  access-key: ${AWS_ACCESS_KEY:}
  secret-key: ${AWS_SECRET_KEY:}
  region: ${AWS_REGION:us-east-1}
  s3:
    bucket: ${AWS_S3_BUCKET:ridelist-media}
```

### Beans Provided

- `S3Client` - For standard S3 operations (upload, delete, etc.)
- `S3Presigner` - For generating presigned URLs

### Image Constraints

- Max 10 images per listing
- Max 5MB per image
- Allowed types: JPEG, PNG, WebP

## Conventions

### Naming
- Entities: singular (User, Listing)
- Tables: plural snake_case (users, listings)
- Enums: UPPER_SNAKE_CASE values
- DTOs: `*Request` for input, `*Response` for output

### MapStruct
- Use `@Mapper(componentModel = "spring")` 
- Combine with `UserMapper`, `ListingImageMapper`, `LocationMapper`, `CategorizationMapper` via `uses = {}`
- Ignore auto-managed fields (id, timestamps, relationships)

### Repositories
- Extend `JpaRepository<Entity, UUID>`
- Return `Page<T>` for list queries
- Use `@Query` for complex filtering
- Add `@Modifying` for update/delete queries

## What's Implemented

- Domain model (entities, enums)
- Database migrations
- Repositories with query methods
- DTOs (request/response) - updated with listingType, vehicleType, state, part fields
- MapStruct mappers - updated with updateEntityFromRequest method
- Security configuration (JWT)
- AWS S3 configuration
- Global exception handling (ResourceNotFoundException, BadRequestException, DuplicateResourceException)
- **In-memory caching** for reference data (locations, categorization)
- **MarketplaceListingService** - core listing operations

### MarketplaceListingService

Location: `service/MarketplaceListingService.java`

Provides listing management functionality:
- `createListing(CreateListingRequest, UUID sellerId)` - Create draft listing
- `updateListing(UUID listingId, UpdateListingRequest, UUID sellerId)` - Update existing listing
- `publishListing(UUID listingId, UUID sellerId)` - Publish draft listing (DRAFT → ACTIVE)
- `markAsSold(UUID listingId, UUID sellerId)` - Mark as sold (ACTIVE → SOLD)
- `getListings(filters, Pageable)` - Get listings with filters + pagination
- `getListingById(UUID)` - Get single listing
- `getSellerListings(UUID sellerId, Pageable)` - Get all listings for a seller

**Filtering Options:**
- `listingType` (VEHICLE, PART)
- `vehicleType` (MOTORCYCLE, TRICYCLE, BICYCLE)
- `stateId`, `axisId`, `areaId` (UUID - location hierarchy)
- `minPrice` / `maxPrice`

**Listing Status Flow:**
```
DRAFT → ACTIVE → SOLD
           ↓
        DELETED
```

**Validation:**
- Vehicle listings require `vehicleType`
- Vehicle listings can optionally include `makeId`, `vehicleModelId`, `modelYearId` (categorization)
- Part listings require `partName`
- Publishing requires title, price > 0, and state

**Categorization Validation:**
- `vehicleModelId` must belong to the selected `makeId`
- `modelYearId` must belong to the selected `vehicleModelId`

### AuthService

Location: `service/AuthService.java`

Handles user authentication:
- `register(RegisterRequest)` - Register new user with BCrypt password hashing, returns JWT tokens
- `login(LoginRequest)` - Authenticate user, returns JWT tokens

**Response:** `AuthResponse` containing accessToken, refreshToken, tokenType, expiresIn, and user details.

### MessageService

Location: `service/MessageService.java`

Handles buyer inquiries/contact requests:
- `sendInquiry(UUID listingId, ContactSellerRequest, UUID buyerId)` - Send inquiry (supports authenticated + guest users)
- `getMessagesForSeller(UUID sellerId, Pageable)` - Get all inquiries for seller's listings
- `getMessagesForListing(UUID listingId, UUID sellerId, Pageable)` - Get inquiries for specific listing

**Guest Inquiry Support:**
- Guest users must provide `senderName` and `senderPhone`
- Authenticated users auto-populate from their profile

**Validation:**
- Cannot inquire on non-active listings
- Cannot inquire on your own listing
- One inquiry per user per listing (authenticated users only)

### CustomUserDetailsService

Location: `service/CustomUserDetailsService.java`

Implements Spring Security's `UserDetailsService`:
- `loadUserByUsername(email)` - Load user by email for authentication
- `loadUserById(UUID)` - Load user by ID for JWT validation

### FavoriteService

Location: `service/FavoriteService.java`

Manages user's saved/favorited listings:
- `addToFavorites(UUID userId, UUID listingId)` - Add listing to user's favorites
- `removeFromFavorites(UUID userId, UUID listingId)` - Remove listing from favorites
- `getUserFavorites(UUID userId, Pageable)` - Get paginated list of user's favorites
- `isFavorite(UUID userId, UUID listingId)` - Check if listing is favorited
- `getFavoriteCount(UUID listingId)` - Get total favorites count for a listing

**Validation:**
- Cannot favorite deleted listings
- Cannot favorite your own listing
- Duplicate favorites prevented (unique constraint)

### AccountService

Location: `service/AccountService.java`

Handles user account management, including soft delete:
- `deleteAccount(UUID userId)` - Soft delete user account

**Delete Account Flow:**
1. Validate user exists and is not already deleted
2. Disable account (`enabled = false`, `deletedAt = now`)
3. Mark all user's listings as `DELETED` (except already SOLD/DELETED)
4. Delete all user's favorites
5. Messages are preserved for historical records

**Validation:**
- Cannot delete already deleted account
- Returns `BadRequestException` if account is already deleted/disabled

**Soft Delete Implementation:**
- User entity uses `@Where(clause = "enabled = true AND deleted_at IS NULL")` for global filtering
- Deleted users are automatically excluded from all JPA queries
- Authentication explicitly checks `deletedAt` to reject deleted user logins
- Listing queries join with seller and filter out deleted sellers

### S3Service

Location: `service/S3Service.java`

Handles file uploads to AWS S3:
- `uploadFile(MultipartFile file, String folder)` - Upload file, returns URL
- `deleteFile(String key)` - Delete file by S3 key
- `deleteFileByUrl(String fileUrl)` - Delete file by full URL

**File Path Structure:**
```
/listings/{listingId}/{uuid}.{extension}
```

**Rules:**
- Max file size: 5MB
- Allowed types: JPG, PNG only
- Filenames: UUID-generated for uniqueness

**URL Format:**
```
https://{bucket}.s3.{region}.amazonaws.com/{key}
```

### ImageService

Location: `service/ImageService.java`

Manages listing images with S3 storage and database metadata:
- `uploadListingImages(UUID listingId, List<MultipartFile>, UUID sellerId)` - Upload multiple images
- `deleteImage(UUID imageId, UUID sellerId)` - Delete single image
- `getListingImages(UUID listingId)` - Get all images for a listing
- `setPrimaryImage(UUID imageId, UUID sellerId)` - Set image as primary

**Configuration** (`ridelist.image.*`):
```yaml
ridelist.image.min-count: 1      # Minimum images per listing
ridelist.image.max-count: 10     # Maximum images per listing
ridelist.image.allowed-types: image/jpeg,image/png,image/webp
ridelist.image.max-size-mb: 5
```

**Features:**
- First uploaded image automatically set as primary
- Auto-assigns new primary when current primary is deleted
- Validates min/max image count constraints
- Stores metadata in `listing_images` table (URL, S3 key, display order, primary flag)

### LocationService

Location: `service/LocationService.java`

Admin-only service for managing location hierarchy (State → Axis → Area). All write operations automatically invalidate the location cache.

**State Operations:**
- `createState(CreateStateRequest)` - Create state with auto-generated slug
- `updateState(UUID, UpdateLocationRequest)` - Update state name/slug
- `deleteState(UUID)` - Delete state (cascades)
- `getAllStates()` - List all states

**Axis Operations:**
- `createAxis(CreateAxisRequest)` - Create axis under a state
- `updateAxis(UUID, UpdateLocationRequest)` - Update axis name/slug
- `deleteAxis(UUID)` - Delete axis (cascades)
- `getAxesByState(UUID stateId)` - List axes for a state

**Area Operations:**
- `createArea(CreateAreaRequest)` - Create area under an axis
- `updateArea(UUID, UpdateLocationRequest)` - Update area name/slug
- `deleteArea(UUID)` - Delete area
- `getAreasByAxis(UUID axisId)` - List areas for an axis

**Validation:**
- Slug uniqueness enforced (throws `DuplicateResourceException`)
- Parent entity existence validated (throws `ResourceNotFoundException`)

**Cache Integration:** All create/update/delete operations call `cache.evictAll()` to invalidate cached location data.

### AttributeService

Location: `service/AttributeService.java`

Admin service for managing attribute definitions:
- `createAttribute(AttributeCreateRequest)` - Create attribute with auto-generated slug
- `updateAttribute(UUID, AttributeUpdateRequest)` - Update attribute properties
- `getAttributesByListingType(ListingType)` - Get all attributes for a listing type
- `getActiveAttributesByListingType(ListingType)` - Get only active attributes
- `getFilterableAttributes(ListingType)` - Get filterable and active attributes
- `getAttributeById(UUID)` - Get single attribute
- `deleteAttribute(UUID)` - Delete attribute (cascades to listing values)

**Validation:**
- Slug uniqueness enforced (throws `DuplicateResourceException`)

### ListingAttributeService

Location: `service/ListingAttributeService.java`

Manages attribute values on listings:
- `saveAttributes(Listing, List<AttributeValueRequest>)` - Save/replace attributes on a listing
- `getAttributesForListing(UUID listingId)` - Get all attribute values for a listing
- `deleteAttributesForListing(UUID listingId)` - Delete all attributes for a listing

**Validation:**
- No duplicate attribute IDs in request
- All attributes must exist and be active
- Attribute must match listing's listingType
- Required attributes must be provided

## In-Memory Cache System

RideList uses a simple, thread-safe in-memory caching system for reference data to reduce database calls and improve response times. This is used for data that changes infrequently (locations, vehicle categorization).

### Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Frontend Client                              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    LookupController                              │
│                   GET /api/v1/lookup/*                           │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              ▼                               ▼
┌──────────────────────────┐    ┌──────────────────────────┐
│  LocationCacheService    │    │ CategorizationCacheService│
│  - getStates()           │    │  - getMakes()            │
│  - getAxesByState()      │    │  - getModelsByMake()     │
│  - getAreasByAxis()      │    │  - getYearsByModel()     │
└──────────────────────────┘    └──────────────────────────┘
              │                               │
              └───────────────┬───────────────┘
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      InMemoryCache                               │
│  - ConcurrentHashMap<String, CacheEntry>                        │
│  - 10-hour TTL                                                   │
│  - Lazy loading via Supplier                                     │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    PostgreSQL Database                           │
│  (only on cache miss or expiration)                             │
└─────────────────────────────────────────────────────────────────┘
```

### InMemoryCache

Location: `cache/InMemoryCache.java`

A generic, thread-safe cache component using `ConcurrentHashMap`.

**Features:**
- Lazy loading - data fetched only on first request
- Thread-safe operations via `ConcurrentHashMap.compute()`
- 10-hour TTL with automatic expiration
- Manual eviction support

**Methods:**
| Method | Description |
|--------|-------------|
| `<T> T get(String key, Supplier<T> supplier)` | Get cached value, compute if absent/expired |
| `void evict(String key)` | Remove single entry |
| `void evictAll()` | Clear entire cache |
| `int size()` | Get number of cached entries |
| `boolean containsKey(String key)` | Check if key exists and is not expired |

### SimpleNode DTO

Location: `dto/response/SimpleNode.java`

Lightweight DTO for cached reference data. Used instead of JPA entities to avoid session issues.

```java
public class SimpleNode {
    UUID id;
    String name;
    String slug;
}
```

### LocationCacheService

Location: `service/LocationCacheService.java`

Cached access to location hierarchy (State → Axis → Area).

| Method | Cache Key | Description |
|--------|-----------|-------------|
| `getStates()` | `states:all` | All Nigerian states |
| `getAxesByState(UUID)` | `state:{id}:axes` | Axes for a state |
| `getAreasByAxis(UUID)` | `axis:{id}:areas` | Areas for an axis |

### CategorizationCacheService

Location: `service/CategorizationCacheService.java`

Cached access to vehicle categorization (Make → VehicleModel → ModelYear).

| Method | Cache Key | Description |
|--------|-----------|-------------|
| `getMakes()` | `makes:all` | All vehicle manufacturers |
| `getModelsByMake(UUID)` | `make:{id}:models` | Models for a make |
| `getYearsByModel(UUID)` | `model:{id}:years` | Years for a model |

### Cache Invalidation

Cache is automatically invalidated when admin modifies data via `LocationService`:

| Operation | Invalidation |
|-----------|--------------|
| `createState()` | `cache.evictAll()` |
| `updateState()` | `cache.evictAll()` |
| `deleteState()` | `cache.evictAll()` |
| `createAxis()` | `cache.evictAll()` |
| `updateAxis()` | `cache.evictAll()` |
| `deleteAxis()` | `cache.evictAll()` |
| `createArea()` | `cache.evictAll()` |
| `updateArea()` | `cache.evictAll()` |
| `deleteArea()` | `cache.evictAll()` |

**Note:** When implementing admin categorization service, add the same `cache.evictAll()` pattern for Make/VehicleModel/ModelYear operations.

### Usage Example

```java
// In a service
@Autowired
private InMemoryCache cache;

public List<SimpleNode> getStates() {
    return cache.get("states:all", () -> {
        return stateRepository.findAll().stream()
            .map(s -> SimpleNode.builder()
                .id(s.getId())
                .name(s.getName())
                .slug(s.getSlug())
                .build())
            .toList();
    });
}
```

### Frontend Usage

```javascript
// Build location dropdowns
const states = await fetch('/api/v1/lookup/states').then(r => r.json());
const axes = await fetch(`/api/v1/lookup/states/${stateId}/axes`).then(r => r.json());
const areas = await fetch(`/api/v1/lookup/axes/${axisId}/areas`).then(r => r.json());

// Build vehicle categorization dropdowns
const makes = await fetch('/api/v1/lookup/makes').then(r => r.json());
const models = await fetch(`/api/v1/lookup/makes/${makeId}/models`).then(r => r.json());
const years = await fetch(`/api/v1/lookup/models/${modelId}/years`).then(r => r.json());
```

## Controllers

### AuthController

Location: `controller/AuthController.java`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login and get JWT tokens |

**Request/Response:**
- Register: `RegisterRequest` → `AuthResponse`
- Login: `LoginRequest` → `AuthResponse`

### ListingController

Location: `controller/ListingController.java`

**Public Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/listings` | Get listings with filters + pagination |
| GET | `/api/v1/listings/{id}` | Get listing by ID |

**Query Parameters for GET /api/v1/listings:**
- `listingType` - VEHICLE, PART
- `vehicleType` - MOTORCYCLE, TRICYCLE, BICYCLE
- `stateId`, `axisId`, `areaId` - Location hierarchy (UUIDs)
- `minPrice`, `maxPrice` - Price range
- `attr_{slug}` - Dynamic attribute filters (e.g., `attr_engine-type=150cc`)
- `page`, `size`, `sort` - Pagination (default: size=20, sort=createdAt,desc)

**Authenticated Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/account/listings` | Get current user's listings |
| POST | `/api/v1/account/listings` | Create new listing (draft) |
| PUT | `/api/v1/account/listings/{id}` | Update listing |
| POST | `/api/v1/account/listings/{id}/publish` | Publish draft listing |
| POST | `/api/v1/account/listings/{id}/mark-sold` | Mark listing as sold |
| POST | `/api/v1/account/listings/{id}/images` | Upload images (multipart/form-data) |
| DELETE | `/api/v1/account/listings/images/{imageId}` | Delete image |

**Image Upload:**
```
POST /api/v1/account/listings/{id}/images
Content-Type: multipart/form-data

files: [file1, file2, ...]  # Multiple files supported
```
Returns: `List<ListingImageResponse>` with URLs

### MessageController

Location: `controller/MessageController.java`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/listings/{id}/inquire` | Send inquiry to seller (auth optional) |
| GET | `/api/v1/account/messages` | Get messages received as seller |

**Inquiry Request:**
- Authenticated users: `message` only (name/phone auto-filled)
- Guest users: `senderName`, `senderPhone`, `message` required

### FavoriteController

Location: `controller/FavoriteController.java`

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/account/favorites/{listingId}` | Add listing to favorites |
| DELETE | `/api/v1/account/favorites/{listingId}` | Remove listing from favorites |
| GET | `/api/v1/account/favorites` | Get user's favorite listings |

### AccountController

Location: `controller/AccountController.java`

| Method | Endpoint | Description |
|--------|----------|-------------|
| DELETE | `/api/v1/account` | Delete authenticated user's account (soft delete) |

**Delete Account Response:**
```json
{
  "success": true,
  "message": "Account deleted successfully",
  "timestamp": "2024-01-15T10:30:45.123"
}
```

**Effects of Account Deletion:**
- User cannot log in or access the system
- User's listings become invisible in public queries
- User's favorites are deleted
- User's messages are preserved
- All data is retained for historical records (soft delete)

### AdminLocationController (ADMIN ONLY)

Location: `controller/AdminLocationController.java`

Base path: `/api/v1/admin/locations`

Requires `ROLE_ADMIN`. Uses `@PreAuthorize("hasRole('ADMIN')")`.

**State Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/states` | Create a new state |
| PUT | `/states/{id}` | Update state name |
| DELETE | `/states/{id}` | Delete state (cascades to axes/areas) |
| GET | `/states` | Get all states |

**Axis Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/axes` | Create axis (requires stateId) |
| PUT | `/axes/{id}` | Update axis name |
| DELETE | `/axes/{id}` | Delete axis (cascades to areas) |
| GET | `/states/{stateId}/axes` | Get axes for a state |

**Area Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/areas` | Create area (requires axisId) |
| PUT | `/areas/{id}` | Update area name |
| DELETE | `/areas/{id}` | Delete area |
| GET | `/axes/{axisId}/areas` | Get areas for an axis |

**Request DTOs:**
- `CreateStateRequest` - `{ name }`
- `CreateAxisRequest` - `{ name, stateId }`
- `CreateAreaRequest` - `{ name, axisId }`
- `UpdateLocationRequest` - `{ name }` (used for all update operations)

**Validation:**
- Slugs auto-generated using `SlugUtil.toSlug(name)`
- Duplicate names (slugs) throw `DuplicateResourceException` (HTTP 409)
- Invalid parent IDs throw `ResourceNotFoundException` (HTTP 404)

### AdminAttributeController (ADMIN ONLY)

Location: `controller/AdminAttributeController.java`

Base path: `/api/v1/admin/attributes`

Requires `ROLE_ADMIN`. Uses `@PreAuthorize("hasRole('ADMIN')")`.

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create a new attribute |
| PUT | `/{id}` | Update attribute (name, filterable, required, active) |
| GET | `/` | Get all attributes (optional `listingType` filter) |
| GET | `/{id}` | Get attribute by ID |
| DELETE | `/{id}` | Delete attribute |

**Request DTOs:**
- `AttributeCreateRequest` - `{ name, listingType, filterable?, required? }`
- `AttributeUpdateRequest` - `{ name?, filterable?, required?, active? }`

### PublicAttributeController

Location: `controller/PublicAttributeController.java`

Base path: `/api/v1/attributes`

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/?listingType=VEHICLE` | Get active attributes for listing type |
| GET | `/filterable?listingType=VEHICLE` | Get filterable attributes for listing type |

### LookupController

Location: `controller/LookupController.java`

Base path: `/api/v1/lookup`

Public endpoints for frontend clients to fetch cached reference data for UI construction (dropdowns, filters, etc.).

**Location Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/states` | Get all states (cached) |
| GET | `/states/{stateId}/axes` | Get axes for a state (cached) |
| GET | `/axes/{axisId}/areas` | Get areas for an axis (cached) |

**Categorization Endpoints:**

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/makes` | Get all vehicle makes (cached) |
| GET | `/makes/{makeId}/models` | Get vehicle models for a make (cached) |
| GET | `/models/{modelId}/years` | Get model years for a vehicle model (cached) |

**Response DTO:** `SimpleNode` with fields `id`, `name`, `slug`

**Example Response:**
```json
{
  "success": true,
  "data": [
    { "id": "uuid", "name": "Lagos", "slug": "lagos" },
    { "id": "uuid", "name": "Abuja", "slug": "abuja" }
  ]
}
```

**Caching:**
- Data is lazy-loaded on first request
- Cached for 10 hours (TTL)
- Automatically invalidated when admin modifies location or categorization data

## Logging & Monitoring

### Overview

RideList uses structured JSON logging for Kubernetes compatibility and Spring Boot Actuator for health checks and metrics.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Kubernetes / EKS                              │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────┐  │
│  │ Fluent Bit  │ ←─ │   STDOUT    │ ←─ │   RideList App      │  │
│  │ (collector) │    │ (JSON logs) │    │   (Logback JSON)    │  │
│  └─────────────┘    └─────────────┘    └─────────────────────┘  │
│         │                                        │               │
│         ▼                                        ▼               │
│  ┌─────────────┐                         ┌─────────────────┐    │
│  │ CloudWatch  │                         │ /actuator/health│    │
│  │   Logs      │                         │ (K8s probes)    │    │
│  └─────────────┘                         └─────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Structured JSON Logging

**Configuration:** `src/main/resources/logback-spring.xml`

Logs are JSON-formatted for easy parsing by log aggregators (Fluent Bit, CloudWatch, etc.).

**Log Fields:**
| Field | Description |
|-------|-------------|
| `timestamp` | UTC timestamp |
| `level` | Log level (INFO, WARN, ERROR, DEBUG) |
| `service` | Application name ("ridelist") |
| `traceId` | Correlation ID for request tracing |
| `userId` | Current user ID (when available) |
| `listingId` | Current listing ID (when available) |
| `thread` | Thread name |
| `logger` | Logger class name |
| `message` | Log message |

**Example JSON Log:**
```json
{
  "timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "service": "ridelist",
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "123e4567-e89b-12d3-a456-426614174000",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.ridelist.service.AuthService",
  "message": "User registered successfully"
}
```

**Profile-based Configuration:**
| Profile | Format | Log Level |
|---------|--------|-----------|
| `prod`, `staging`, `k8s` | JSON to STDOUT | INFO |
| `default`, `dev`, `local` | Plain text | DEBUG |
| `test` | Plain text (minimal) | WARN |

### Correlation ID (Request Tracing)

**Component:** `filter/CorrelationIdFilter.java`

Every request gets a unique `traceId` for end-to-end tracing:
1. Filter generates UUID (or uses incoming `X-Trace-Id` header)
2. Stored in MDC for automatic inclusion in all logs
3. Returned in `X-Trace-Id` response header

**Usage:**
```java
// traceId is automatically included in all logs within the request
log.info("Processing listing creation");  // traceId in output

// Get current traceId programmatically
String traceId = LogContext.getTraceId();
```

### Adding Context to Logs

**Utility:** `util/LogContext.java`

Add contextual information to logs within a request:

```java
// In a service method
public void processListing(UUID listingId, UUID userId) {
    LogContext.setUserId(userId);
    LogContext.setListingId(listingId);
    
    try {
        log.info("Starting listing process");  // userId & listingId in output
        // ... business logic
    } finally {
        LogContext.clearAll();
    }
}
```

### Logging Best Practices

**DO:**
```java
// Parameterized logging (efficient - no string concat if level disabled)
log.info("User {} registered successfully", userId);
log.debug("Listing {} updated with price {}", listingId, price);

// Log key events
log.info("User registered: {}", email);
log.info("Login attempt for: {}", email);
log.info("Listing created: {} by seller {}", listingId, sellerId);
log.info("Image uploaded for listing: {}", listingId);
log.error("Failed to process payment", exception);
```

**DON'T:**
```java
// Never log sensitive data
log.info("Password: " + password);        // NEVER
log.info("JWT token: {}", token);          // NEVER
log.debug("Credit card: {}", cardNumber);  // NEVER
```

### Spring Boot Actuator

**Endpoints enabled:**

| Endpoint | URL | Description |
|----------|-----|-------------|
| Health | `/actuator/health` | Application health status |
| Liveness | `/actuator/health/liveness` | Kubernetes liveness probe |
| Readiness | `/actuator/health/readiness` | Kubernetes readiness probe |
| Info | `/actuator/info` | Application information |
| Metrics | `/actuator/metrics` | Micrometer metrics |

**Kubernetes Probe Configuration:**
```yaml
# Example Kubernetes deployment
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

**Health Check Response:**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### Metrics (Micrometer)

Default metrics available at `/actuator/metrics`:
- `http.server.requests` - HTTP request count, latency
- `jvm.memory.used` - JVM memory usage
- `jvm.gc.pause` - Garbage collection pauses
- `system.cpu.usage` - CPU usage
- `hikaricp.connections.active` - Database connection pool

**Example metric query:**
```
GET /actuator/metrics/http.server.requests
GET /actuator/metrics/http.server.requests?tag=uri:/api/v1/listings
```

## OpenAPI / Swagger

API documentation is available via Springdoc OpenAPI.

**URLs:**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`

**Configuration:** `config/OpenApiConfig.java`

**Features:**
- JWT Bearer authentication support (click "Authorize" button)
- Try-it-out enabled for all endpoints
- Operations sorted by HTTP method
- Request duration display

## What Needs Implementation

1. **Services**: UserService (profile management, user lookup)
2. **Controllers**: UserController (profile endpoints)

## Build & Run

```bash
# Build
mvn clean package

# Run (requires PostgreSQL)
mvn spring-boot:run

# Run with env vars
DB_HOST=localhost DB_NAME=ridelist JWT_SECRET=your-secret mvn spring-boot:run
```

## Testing

### Configuration

Test configuration file: `src/test/resources/application-test.properties`

- **PostgreSQL via Testcontainers** (real database, not H2)
- Flyway enabled for schema migrations
- Mock AWS S3 credentials
- JWT test secret for authentication

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=AuthControllerIntegrationTest

# Run multiple test classes
mvn test -Dtest=ListingControllerIntegrationTest,FavoriteControllerIntegrationTest

# Run with test profile explicitly
mvn test -Dspring.profiles.active=test
```

### Test Structure

```
src/test/java/com/ridelist/
└── integration/
    ├── BaseIntegrationTest.java                    # Base class with Testcontainers setup
    ├── AuthControllerIntegrationTest.java          # 14 tests
    ├── ListingControllerIntegrationTest.java       # 26 tests
    ├── FavoriteControllerIntegrationTest.java      # 15 tests
    ├── ImageServiceIntegrationTest.java            # 17 tests (uses @MockBean S3Service)
    ├── MessageControllerIntegrationTest.java       # 16 tests
    ├── CacheBehaviorIntegrationTest.java           # 8 tests
    ├── DataIntegrityIntegrationTest.java           # 20 tests
    ├── AdminAttributeControllerIntegrationTest.java # 22 tests
    ├── AdminLocationControllerIntegrationTest.java  # 28 tests
    └── PerformanceSanityTest.java                  # 5 tests
```

### Base Integration Test

All integration tests extend `BaseIntegrationTest` which provides:
- `@SpringBootTest` with random port
- `@AutoConfigureMockMvc` for MockMvc injection
- `@ActiveProfiles("test")` to use test configuration
- `@Testcontainers` with PostgreSQL 15 container
- `@Transactional` for test isolation (auto-rollback)
- `@DynamicPropertySource` to inject container JDBC URL

**Helper Methods:**
- `registerAndGetToken(email, password)` - Register user and return JWT
- `loginAndGetToken(email, password)` - Login and return JWT
- `createTestUser(email, role)` - Create user directly in DB (unusable password)
- `createTestListing(seller, type)` - Create draft listing
- `createTestState(name)` - Create state entity
- `createTestAxis(name, state)` - Create axis entity
- `createTestArea(name, axis)` - Create area entity
- `createTestAttribute(name, type, filterable)` - Create attribute definition
- `authHeader(token)` - Format Bearer token header

### Test Coverage

| Controller/Service | Tests | Description |
|--------------------|-------|-------------|
| AuthController | 14 | Register, Login, JWT validation, access control |
| ListingController | 26 | CRUD, publish, mark-sold, filters, pagination, ownership |
| FavoriteController | 15 | Add, remove, list, duplicates, auth checks, pagination |
| ImageService | 17 | Upload, delete, primary image, validation, S3 mocking |
| MessageController | 16 | Send inquiry (auth/guest), get messages, validation |
| CacheBehavior | 8 | Cache hit/miss, eviction on admin ops, access control |
| DataIntegrity | 20 | Cascades, unique constraints, FK validation, NOT NULL |
| AdminAttributeController | 22 | CRUD attributes, public endpoints, auth/access control |
| AdminLocationController | 28 | State/Axis/Area CRUD, cascades, public lookup |
| PerformanceSanity | 5 | Response time sanity checks, pagination, caching |

**Total: 171 integration tests**

### Key Testing Notes

1. **Authorization returns 401 for non-owner access**: When a user tries to modify a listing they don't own, `MarketplaceListingService` throws `UnauthorizedException` (401), not `AccessDeniedException` (403).

2. **Duplicate favorites return 400**: `FavoriteService` throws `BadRequestException` for duplicate favorites, not `DuplicateResourceException`.

3. **Listing requires `category` field**: The `category` column is NOT NULL. Test helpers must set it:
   - Vehicles: `ListingCategory.MOTORCYCLE`, `TRICYCLE`, or `BICYCLE`
   - Parts: `ListingCategory.SPARE_PART` or `ACCESSORY`

4. **Windows Docker Desktop**: Tests configure `npipe:////./pipe/docker_engine` for Testcontainers on Windows.

5. **Image tests use @MockBean S3Service**: `ImageServiceIntegrationTest` mocks S3 to avoid real AWS calls. Configure mock behavior in `@BeforeEach`.

6. **Inquiry endpoints return 201 Created**: `MessageController.sendInquiry()` returns 201, not 200.

7. **Duplicate inquiry returns 400**: `MessageService` throws `BadRequestException` for duplicate inquiries (authenticated users only), not 409.

8. **Creating admin users for tests**: `createTestUser()` uses placeholder password that can't be used for login. For admin tests:
   ```java
   String token = registerAndGetToken("admin@test.com", "password123");
   User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
   admin.setRole(Role.ADMIN);
   userRepository.save(admin);
   token = loginAndGetToken("admin@test.com", "password123"); // Re-login for fresh token
   ```

9. **Testing cascade deletes**: Database has `ON DELETE CASCADE` on FK constraints. In `@Transactional` tests, use `entityManager.clear()` after flush to see cascade effects:
   ```java
   @Autowired EntityManager entityManager;
   
   listingRepository.delete(listing);
   listingRepository.flush();
   entityManager.clear(); // Clear JPA cache to see DB state
   assertThat(listingImageRepository.findById(imageId)).isEmpty();
   ```

## Common Tasks

### Adding a new entity
1. Create entity in `model/`
2. Create Flyway migration `V{n}__description.sql`
3. Create repository in `repository/`
4. Create request/response DTOs
5. Create MapStruct mapper
6. Create service
7. Create controller

### Adding query to repository
```java
// Derived query
Page<Listing> findByStateId(UUID stateId, Pageable pageable);

// Custom JPQL
@Query("SELECT l FROM Listing l WHERE l.price BETWEEN :min AND :max")
Page<Listing> findByPriceRange(@Param("min") BigDecimal min, @Param("max") BigDecimal max, Pageable pageable);
```

### Using SlugUtil
```java
// Convert name to URL-safe slug
String slug = SlugUtil.toSlug("Lagos Mainland");  // "lagos-mainland"

// Convert slug back to readable name
String name = SlugUtil.fromSlug("lagos-mainland");  // "Lagos Mainland"
```

### Getting current user in controller
```java
@GetMapping("/me")
public ApiResponse<UserResponse> getCurrentUser(@CurrentUser UserPrincipal principal) {
    // principal.getId() gives UUID
}
```
