# RideList SEO Features

This document describes the SEO optimizations implemented in the RideList API.

## 1. Listing Title Length Constraint

### Purpose
Titles must be between 45 and 150 characters for optimal SEO effectiveness:
- **Too short (<45 chars)**: Insufficient keyword signal for search engines
- **Too long (>150 chars)**: Truncated in Google SERP results

### Implementation

**Entity Constraint** (`model/Listing.java`):
```java
@Column(name = "title", nullable = false, length = 150)
private String title;
```

**Request Validation** (`CreateListingRequest.java`):
```java
@NotBlank(message = "Title is required")
@Size(min = 45, max = 150, 
      message = "Title must be between 45 and 150 characters. Include make, model, year, condition and location for best results.")
private String title;
```

**Update Validation** (`UpdateListingRequest.java`):
```java
@Size(min = 45, max = 150, 
      message = "Title must be between 45 and 150 characters.")
private String title;  // Nullable on update — only validated if provided
```

### Validation Message
The create endpoint provides actionable guidance:
> "Title must be between 45 and 150 characters. Include make, model, year, condition and location for best results."

### Database Migration
`V13__listing_title_length.sql`:
- Alters column type to VARCHAR(150)
- Logs existing titles outside range (legacy data preserved)
- Does NOT reject existing listings outside range

---

## 2. SEO-Optimized Seller Profile URLs

### URL Pattern

**Current (UUID-based)**:
```
/seller/f630be62-b1bb-4100-840f-266ef035b307
```

**Target (SEO-friendly)**:
```
/sellers/chukwuemeka-autos-lagos-f630be
        ↑ slugged name    ↑ location  ↑ 6-char UUID fragment
```

If no location:
```
/sellers/chukwuemeka-autos-f630be
```

### Resolution Strategy

The 6-character UUID fragment guarantees uniqueness — no slug collision possible. Resolution uses the UUID fragment, not the name slug. This means:

- Changing name/location updates the URL
- Old URLs still resolve (UUID fragment never changes)
- No redirect logic needed

### Database Fields

Added to `users` table via `V14__seller_profile_fields.sql`:

| Column | Type | Description |
|--------|------|-------------|
| `bio` | TEXT | Seller bio/description (max 500 chars) |
| `seller_slug` | VARCHAR(200) | SEO-friendly seller URL slug |
| `seller_slug_updated_at` | TIMESTAMP | Last slug update time |

### Slug Generation

Generated via `SlugUtil.toSellerSlug()`:
```java
String slug = SlugUtil.toSellerSlug(
    "Chukwuemeka",    // firstName
    "Autos",          // lastName
    "lagos",          // stateSlug (nullable)
    userId            // UUID
);
// Result: "chukwuemeka-autos-lagos-f630be"
```

### Slug Generation Triggers

1. **On registration as DEALER**: `AuthService.register()` calls `sellerProfileService.generateSellerSlug()`
2. **On profile update**: `UserService.updateProfile()` regenerates slug when firstName, lastName, or state changes

### API Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/sellers/{sellerSlug}` | Get seller profile by SEO slug |
| `GET /api/v1/seller/{uuid}` | Backward compatibility endpoint |
| `GET /api/v1/sellers/sitemap-data` | Seller pages for sitemap |

### Response DTO

`SellerProfileResponse`:
```json
{
  "id": "uuid",
  "firstName": "Chukwuemeka",
  "lastName": "Autos",
  "bio": "Premium vehicle dealer in Lagos",
  "sellerSlug": "chukwuemeka-autos-lagos-f630be",
  "sellerUrl": "/sellers/chukwuemeka-autos-lagos-f630be",
  "accountType": "DEALER",
  "state": "Lagos",
  "activeListingCount": 15,
  "memberSince": "Member since Jan 2024",
  "hasPublicPage": true,
  "profileImageUrl": "https://..."
}
```

### Bio Field

- Added to `UpdateProfileRequest` with `@Size(max = 500)` validation
- Updated via `PUT /api/v1/account/me`
- Included in `UserResponse` and `SellerProfileResponse`

### Public Page Logic

`hasPublicPage` is true when:
- User has `accountType = DEALER`
- User has 1+ active/published listings

### Security

Public seller endpoints added to `SecurityConfig`:
```java
.requestMatchers(HttpMethod.GET, "/api/v1/sellers/**").permitAll()
.requestMatchers(HttpMethod.GET, "/api/v1/seller/**").permitAll()
```

---

## 3. Location Hub Metadata Endpoints

### Purpose

Power SEO-optimized location pages that help users discover listings by geographic area. Each location page shows:
- Listing counts per sub-location
- Nigerian keyword variants for local search (okada, keke napep, tokunbo)
- SEO-friendly canonical URLs

### Endpoint

```
GET /api/v1/listings/browse/{categoryPath}/locations
    ?stateSlug=lagos
    &axisSlug=mainland
```

### Progressive Drill-Down

The endpoint returns the next level of location hierarchy based on provided parameters:

| Parameters | Returns |
|------------|---------|
| None | States with counts |
| `stateSlug` | Axes within that state |
| `stateSlug` + `axisSlug` | Areas within that axis |

### Response DTO

`LocationHubResponse`:
```json
{
  "level": "states",
  "locations": [
    {
      "name": "Lagos",
      "slug": "lagos",
      "url": "/motorcycles/lagos",
      "count": 245
    },
    {
      "name": "Kano",
      "slug": "kano",
      "url": "/motorcycles/kano",
      "count": 89
    }
  ]
}
```

The `level` field indicates which hierarchy tier is returned:
- `"states"` — Top-level Nigerian states
- `"axes"` — Subdivisions within a state (e.g., Mainland, Island)
- `"areas"` — Neighborhoods within an axis (e.g., Ikeja, Surulere)

### URL Construction

URLs are category-relative and built progressively:

| Level | URL Pattern |
|-------|-------------|
| State | `/{categoryPath}/{stateSlug}` |
| Axis | `/{categoryPath}/{stateSlug}/{axisSlug}` |
| Area | `/{categoryPath}/{stateSlug}/{axisSlug}/{areaSlug}` |

Example for motorcycles in Lagos Mainland:
```
/motorcycles/lagos/mainland/ikeja
```

### Nigerian Keyword Variants

The `/meta` endpoint includes localized terms in descriptions:

| Vehicle Type | Nigerian Term |
|--------------|---------------|
| MOTORCYCLE | okada bikes |
| TRICYCLE | keke napep |
| PART | accessories |
| — (all) | tokunbo (fairly used) |

**Example Description**:
```
Browse motorcycles (okada bikes) for sale in Lagos on RideList. 
New and tokunbo (fairly used) options from verified dealers.
```

### Browse Page Meta Endpoint

```
GET /api/v1/listings/browse/{categoryPath}/meta
    ?stateSlug=lagos
    &axisSlug=mainland
    &areaSlug=ikeja
```

Response:
```json
{
  "title": "Motorcycles for Sale in Ikeja, Lagos",
  "description": "Browse motorcycles (okada bikes) for sale in Ikeja, Lagos on RideList. New and tokunbo (fairly used) options from verified dealers.",
  "canonicalUrl": "/motorcycles/lagos/mainland/ikeja",
  "locationLabel": "Ikeja, Lagos",
  "categoryLabel": "Motorcycles"
}
```

### Empty Location Filtering

Locations with zero active listings are automatically excluded from results. This prevents dead links and improves user experience.

### Repository Queries

Three JPQL constructor queries power the location counts:

| Method | Returns |
|--------|---------|
| `countActiveListingsByCategory()` | State-level counts |
| `countActiveListingsByStateAndCategory()` | Axis-level counts |
| `countActiveListingsByAxisAndCategory()` | Area-level counts |

All queries:
- Filter by `listingType` and optional `vehicleType`
- Only count ACTIVE and PUBLISHED listings
- Group by location and order by count DESC

### Frontend Usage

```javascript
// Build location hub page
const { level, locations } = await fetch(
  '/api/v1/listings/browse/motorcycles/locations?stateSlug=lagos'
).then(r => r.json());

// Render sub-location links with counts
locations.forEach(loc => {
  // loc.url: "/motorcycles/lagos/mainland"
  // loc.name: "Mainland"
  // loc.count: 145
});

// Get page meta for SEO
const meta = await fetch(
  '/api/v1/listings/browse/motorcycles/meta?stateSlug=lagos'
).then(r => r.json());

// meta.title: "Motorcycles for Sale in Lagos"
// meta.description: includes "okada bikes" and "tokunbo"
```

### Caching

Location hub counts are cached using `InMemoryCache` since they only change when listings are created or status changes.

**Cache Key Pattern**:
```
locationHub:{categoryPath}-{stateSlug|all}-{axisSlug|all}
```

Examples:
- `locationHub:motorcycles-all-all` — State-level counts for motorcycles
- `locationHub:motorcycles-lagos-all` — Axis-level counts for Lagos motorcycles
- `locationHub:tricycles-lagos-mainland` — Area-level counts for Lagos Mainland tricycles

**Cache Eviction Triggers**:

| Service | Method | Trigger |
|---------|--------|---------|
| `MarketplaceListingService` | `publishListing()` | Listing becomes ACTIVE |
| `MarketplaceListingService` | `markAsSold()` | Listing becomes SOLD |
| `AdminListingService` | `adminChangeListingStatus()` | Any status change |
| `AdminListingService` | `adminDeleteListing()` | Listing set to DELETED |

**TTL**: 10 hours (default `InMemoryCache` TTL)

### Implementation Files

| File | Purpose |
|------|---------|
| `dto/response/LocationCount.java` | Location with name, slug, url, count |
| `dto/response/LocationHubResponse.java` | Level + locations list |
| `service/LocationHubService.java` | Cached location hub logic |
| `repository/ListingRepository.java` | Count queries by location |
| `controller/ListingController.java` | getLocationHub(), getBrowsePageMeta() |

---

## 4. "All Categories" Browse & Location-Only URLs

### Purpose

Support location-only SEO URLs like `/lagos` or `/lagos/ikeja` that show all listing types (vehicles, parts) in that location. This enables:
- Location-focused landing pages without category restriction
- Broader search results for users exploring by location
- Clean URLs that match how users naturally search ("what's available in Ikeja?")

### The "all" Pseudo-Category

The API accepts `all` as a special `categoryPath` value that returns listings of **all types**.

**URL Mapping:**

| Frontend URL | API Call |
|--------------|----------|
| `/all` | `GET /api/v1/listings/browse/all` |
| `/lagos` | `GET /api/v1/listings/browse/all/lagos` |
| `/lagos/ikeja` | `GET /api/v1/listings/browse/all/lagos/ikeja` |
| `/lagos/ikeja/opebi` | `GET /api/v1/listings/browse/all/lagos/ikeja/opebi` |

### Behavior When `categoryPath = "all"`

| Aspect | Behavior |
|--------|----------|
| Listing type filter | **Disabled** — returns VEHICLE and PART listings |
| Vehicle type filter | **Disabled** — returns MOTORCYCLE, TRICYCLE, BICYCLE |
| Location filter | **Applied** — filters by state/axis/area as usual |
| Attribute filters | **Applied** — `attr_*` params still work |
| Price filters | **Applied** — `minPrice`, `maxPrice` still work |
| Text search | **Applied** — `q` param works |
| Pagination | **Applied** — `page`, `size`, `sort` still work |

### SEO-Friendly Canonical URLs

**Critical:** When `categoryPath = "all"`, the `canonicalUrl` field **omits the "all" prefix** for cleaner SEO URLs:

| API Request | `canonicalUrl` in Response |
|-------------|---------------------------|
| `/browse/all/lagos` | `/lagos` |
| `/browse/all/lagos/ikeja` | `/lagos/ikeja` |
| `/browse/motorcycles/lagos` | `/motorcycles/lagos` |

This ensures search engines index clean URLs without exposing the `all` pseudo-category.

### Meta Endpoint Response

```
GET /api/v1/listings/browse/all/meta?stateSlug=lagos&axisSlug=ikeja
```

Response:
```json
{
  "title": "All Listings for Sale in Ikeja, Lagos",
  "description": "Browse motorcycles, tricycles, bicycles, and spare parts for sale in Ikeja, Lagos on RideList. New and tokunbo (fairly used) options from verified dealers.",
  "canonicalUrl": "/lagos/ikeja",
  "locationLabel": "Ikeja, Lagos",
  "categoryLabel": "All Listings"
}
```

### SEO Title Patterns

| URL | Generated Title |
|-----|-----------------|
| `/all` | "All Listings for Sale in Nigeria" |
| `/all/lagos` | "All Listings for Sale in Lagos" |
| `/all/lagos/ikeja` | "All Listings for Sale in Ikeja, Lagos" |
| `/all/lagos/ikeja/opebi` | "All Listings for Sale in Opebi, Lagos" |

### Location Hub URLs

When browsing with `categoryPath = "all"`, the location hub URLs also omit the "all" prefix:

```
GET /api/v1/listings/browse/all/locations
```

Response:
```json
{
  "level": "states",
  "locations": [
    { "name": "Lagos", "slug": "lagos", "url": "/lagos", "count": 1250 },
    { "name": "Abuja", "slug": "abuja", "url": "/abuja", "count": 890 }
  ]
}
```

Note: URLs are `/lagos` not `/all/lagos`.

### Frontend URL Disambiguation

The frontend determines if a URL segment is a category or location:

```typescript
const CATEGORIES = ["motorcycles", "tricycles", "bicycles", "spare-parts", "vehicles", "all"];

function resolveCategory(firstSegment: string): string {
  return CATEGORIES.includes(firstSegment) ? firstSegment : "all";
}

// Examples:
// /motorcycles/lagos → category="motorcycles", stateSlug="lagos"
// /lagos/ikeja       → category="all", stateSlug="lagos", axisSlug="ikeja"
```

### Implementation

**SlugUtil.java:**
```java
public static CategoryResolution resolveCategoryPath(String categoryPath) {
    return switch (categoryPath.toLowerCase()) {
        case "all" -> new CategoryResolution(null, null);  // Both null = no type filter
        case "motorcycles" -> new CategoryResolution(ListingType.VEHICLE, VehicleType.MOTORCYCLE);
        // ... other categories
    };
}
```

**ListingSpecification.java:**
```java
// When listingType is null (from "all" category), skip the type predicates
if (listingType != null) {
    predicates.add(criteriaBuilder.equal(root.get("listingType"), listingType));
}
if (vehicleType != null) {
    predicates.add(criteriaBuilder.equal(root.get("vehicleType"), vehicleType));
}
```

---

## 5. Text Search (`q` Parameter)

### Purpose

Enable keyword search from the homepage search bar across all listings.

### Usage

Add `q` parameter to any browse or listing endpoint:

```
GET /api/v1/listings?q=honda
GET /api/v1/listings/browse/motorcycles?q=honda
GET /api/v1/listings/browse/all/lagos?q=honda
GET /api/v1/listings/browse/motorcycles/lagos/ikeja?q=honda
```

### Search Behavior

| Aspect | Behavior |
|--------|----------|
| Field searched | `title` (case-insensitive) |
| Match type | Partial (LIKE `%query%`) |
| Minimum length | 2 characters (shorter queries ignored) |
| Empty/null `q` | Returns all listings (no search filter) |

### Combined with Other Filters

Text search works with all other filters:

```bash
# Search + location
GET /api/v1/listings?q=honda&location=lagos

# Search + category + location path
GET /api/v1/listings/browse/motorcycles/lagos?q=honda

# Search + all categories + location + attribute
GET /api/v1/listings/browse/all/lagos?q=honda&attr_engine-type=150cc

# Full combination
GET /api/v1/listings/browse/all/lagos/ikeja?q=honda&minPrice=100000&maxPrice=500000
```

### User Journey Example

1. **User types** in homepage search: category="All Categories", state="Lagos", query="honda"
2. **Frontend navigates** to: `/lagos?q=honda`
3. **Frontend calls** API: `GET /api/v1/listings/browse/all/lagos?q=honda`
4. **Backend returns** all listing types in Lagos matching "honda"

### Implementation

**ListingSpecification.java:**
```java
// Text search on title (case-insensitive, partial match)
if (searchQuery != null && searchQuery.length() >= 2) {
    String searchPattern = "%" + searchQuery.toLowerCase() + "%";
    predicates.add(criteriaBuilder.like(
            criteriaBuilder.lower(root.get("title")),
            searchPattern
    ));
}
```

### Database Index (Recommended)

For optimal search performance on large datasets:

```sql
CREATE INDEX idx_listings_title_lower ON listings (LOWER(title));
```

---

## Related Documentation

- `SLUG-ARCHITECTURE.md` — Listing SEO URL architecture
- `SEARCH-ARCHITECTURE.md` — Multi-value filter system
- `CLAUDE.md` — Full API documentation

## Implementation Files

| File | Purpose |
|------|---------|
| `V13__listing_title_length.sql` | Title length migration |
| `V14__seller_profile_fields.sql` | Seller profile fields migration |
| `model/User.java` | Bio, sellerSlug, sellerSlugUpdatedAt fields |
| `util/SlugUtil.java` | toSellerSlug(), extractSellerUuidFragment(), toSellerUrl() |
| `service/SellerProfileService.java` | Slug generation and resolution |
| `controller/SellerController.java` | Public seller endpoints |
| `dto/response/SellerProfileResponse.java` | Seller profile DTO |
| `dto/response/UserResponse.java` | Updated with seller fields |
| `dto/response/UserSummaryResponse.java` | Updated with sellerSlug, sellerUrl |
| `dto/mapper/UserMapper.java` | Seller field mappings |
| `dto/response/LocationCount.java` | Location count DTO |
| `dto/response/LocationHubResponse.java` | Location hub response |
| `dto/response/BrowsePageMeta.java` | Browse page SEO meta |
