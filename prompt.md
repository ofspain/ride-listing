# Backend Requirements: All-Categories Browse & Search

## Overview

This document specifies two related features for browsing and searching across all listing categories (motorcycles, tricycles, spare parts) simultaneously:

1. **Feature A: `all` Pseudo-Category** — Location-only URLs like `/lagos/ikeja` that show all listing types
2. **Feature B: Text Search (`q` param)** — Search by keyword across all categories

---

## Feature A: "all" Pseudo-Category

### Problem

The frontend needs to support location-only URLs like `/lagos/ikeja` that show all listing types in a given location. Currently, all browse endpoints require a specific category.

### Current State

All browse endpoints require a `categoryPath`:

```
GET /api/v1/listings/browse/{categoryPath}
GET /api/v1/listings/browse/{categoryPath}/{locationPath}
GET /api/v1/listings/browse/{categoryPath}/meta
GET /api/v1/listings/browse/{categoryPath}/locations
```

Valid `categoryPath` values: `motorcycles`, `tricycles`, `bicycles`, `spare-parts`, `vehicles`

### Requested Change

Add support for `all` as a special `categoryPath` value that returns listings of ALL types.

### URL Examples

| Frontend URL | Backend API Call |
|--------------|------------------|
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
| Text search | **Applied** — `q` param works (see Feature B) |
| Pagination | **Applied** — `page`, `size`, `sort` still work |

### Endpoints to Update

#### A1. Browse Listings

```
GET /api/v1/listings/browse/all
GET /api/v1/listings/browse/all/{locationPath}
```

**Response:** Same `PagedResponse<ListingSummaryResponse>` as category-specific browse.

**Implementation:**
```java
// In ListingController or ListingSpecification
if ("all".equalsIgnoreCase(categoryPath)) {
    // Skip listingType and vehicleType predicates
} else {
    // Apply category-specific filters as before
}
```

#### A2. Browse Page Meta

```
GET /api/v1/listings/browse/all/meta
GET /api/v1/listings/browse/all/{locationPath}/meta
```

**Response:**
```json
{
  "title": "All Listings in Lagos | RideList",
  "description": "Browse motorcycles, tricycles, and spare parts for sale in Lagos, Nigeria.",
  "canonicalUrl": "https://ridelist.ng/lagos",
  "locationLabel": "Lagos",
  "categoryLabel": "All Categories"
}
```

**SEO Title Patterns:**
| URL | Title |
|-----|-------|
| `/all` | "All Listings for Sale in Nigeria \| RideList" |
| `/all/lagos` | "All Listings in Lagos \| RideList" |
| `/all/lagos/ikeja` | "All Listings in Ikeja, Lagos \| RideList" |
| `/all/lagos/ikeja/opebi` | "All Listings in Opebi, Ikeja \| RideList" |

#### A3. Location Hub

```
GET /api/v1/listings/browse/all/locations
GET /api/v1/listings/browse/all/{locationPath}/locations
```

**Response:** Same `LocationHubResponse` but counts include all listing types.

```json
{
  "level": "states",
  "locations": [
    { "name": "Lagos", "slug": "lagos", "url": "/lagos", "count": 1250 },
    { "name": "Abuja", "slug": "abuja", "url": "/abuja", "count": 890 }
  ]
}
```

**Note:** The `url` field should use the short form `/lagos` not `/all/lagos` for cleaner SEO URLs.

### Canonical URL Generation

When `categoryPath = "all"`, the backend should generate `canonicalUrl` WITHOUT the `all` prefix:

| API Request | `canonicalUrl` in Response |
|-------------|---------------------------|
| `/browse/all/lagos` | `https://ridelist.ng/lagos` |
| `/browse/all/lagos/ikeja` | `https://ridelist.ng/lagos/ikeja` |
| `/browse/motorcycles/lagos` | `https://ridelist.ng/motorcycles/lagos` |

This ensures clean SEO URLs without exposing the `all` pseudo-category to search engines.

### Frontend URL Disambiguation

The frontend will determine if a URL segment is a category or location:

```typescript
const CATEGORIES = ["motorcycles", "tricycles", "bicycles", "spare-parts", "vehicles", "all"];

function resolveCategory(firstSegment: string): string {
  return CATEGORIES.includes(firstSegment) ? firstSegment : "all";
}

// Examples:
// /motorcycles/lagos → category="motorcycles", stateSlug="lagos"
// /lagos/ikeja       → category="all", stateSlug="lagos", axisSlug="ikeja"
```

---

## Feature B: Text Search (`q` Parameter)

### Problem

The homepage search bar allows users to search by keyword (e.g., "honda"). This requires a `q` parameter for full-text search.

### Requested Change

Add a `q` query parameter to browse endpoints for text search across title and description.

### Endpoints to Update

```
GET /api/v1/listings/browse/{categoryPath}?q=honda
GET /api/v1/listings/browse/{categoryPath}/{locationPath}?q=honda
GET /api/v1/listings?q=honda  (existing endpoint)
```

### New Parameter

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `q` | string | No | Search query for full-text search on `title` and `description` fields |

### Search Behavior

- Case-insensitive matching
- Match against `title` field (primary)
- Optionally match against `description` field
- Support partial word matching (e.g., "hon" matches "Honda")
- Results sorted by relevance when `q` is provided, unless explicit `sort` param given
- Empty or missing `q` returns all listings (current behavior)

### Example Requests

```bash
# Search all categories for "honda" in Lagos
GET /api/v1/listings/browse/all/lagos?q=honda

# Search motorcycles for "bajaj" with price filter
GET /api/v1/listings/browse/motorcycles?q=bajaj&minPrice=100000&maxPrice=500000

# Search spare parts for "brake"
GET /api/v1/listings/browse/spare-parts/lagos?q=brake

# Combined: all categories, location, search, attributes
GET /api/v1/listings/browse/all/lagos/ikeja?q=honda&attr_engine-type=150cc
```

### Database Index Recommendation

For optimal search performance:

```sql
-- PostgreSQL full-text index
CREATE INDEX idx_listings_search ON listings 
USING gin(to_tsvector('english', title || ' ' || COALESCE(description, '')));

-- Or simpler LIKE-based index
CREATE INDEX idx_listings_title_lower ON listings (LOWER(title));
```

### JPA Implementation

```java
@Query("""
    SELECT l FROM Listing l 
    WHERE (:query IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :query, '%')))
    AND (:categoryPath = 'all' OR l.category = :resolvedCategory)
    AND (:stateSlug IS NULL OR l.state.slug = :stateSlug)
    AND l.status = 'ACTIVE'
    ORDER BY 
        CASE WHEN :query IS NOT NULL AND LOWER(l.title) LIKE LOWER(CONCAT(:query, '%')) THEN 0 ELSE 1 END,
        l.createdAt DESC
    """)
Page<Listing> searchListings(...);
```

---

## Combined Example

User journey: Search "honda" in Lagos across all categories.

1. **User types** in homepage search: category="All Categories", state="Lagos", query="honda"
2. **Frontend navigates** to: `/lagos?q=honda`
3. **Frontend calls** API: `GET /api/v1/listings/browse/all/lagos?q=honda`
4. **Backend returns** all listing types in Lagos matching "honda"

---

## Implementation Checklist

### Backend — Feature A (all pseudo-category)

- [ ] Update `ListingController.browseListings()` to handle `categoryPath = "all"`
- [ ] Update `ListingController.browseListingsWithLocationPath()` to handle `categoryPath = "all"`
- [ ] Update `ListingController.getBrowsePageMeta()` to handle `categoryPath = "all"`
- [ ] Update `ListingController.getLocationHub()` to handle `categoryPath = "all"`
- [ ] Update `ListingSpecification` to skip type filters when category is "all"
- [ ] Update `BrowsePageMeta` builder for "all" category titles/descriptions
- [ ] Ensure `canonicalUrl` omits "all" prefix in responses

### Backend — Feature B (text search)

- [ ] Add `q` parameter to browse endpoints
- [ ] Implement full-text search on `title` (and optionally `description`)
- [ ] Add database index for search performance
- [ ] Ensure search works with all other filters (location, price, attributes)

### Backend — Testing

- [ ] Test `GET /api/v1/listings/browse/all` returns all types
- [ ] Test `GET /api/v1/listings/browse/all/lagos` filters by state
- [ ] Test `GET /api/v1/listings/browse/all/lagos?q=honda` combines location + search
- [ ] Test `canonicalUrl` omits "all" prefix
- [ ] Test search is case-insensitive
- [ ] Test pagination with search results

### Frontend (after backend is ready)

- [ ] Add routes: `/:stateSlug`, `/:stateSlug/:axisSlug`, `/:stateSlug/:axisSlug/:areaSlug`
- [ ] Add disambiguation logic in route handler
- [ ] Update `useBrowseListings` to pass `q` parameter
- [ ] Update homepage search to navigate to `/lagos?q=query`
- [ ] Update breadcrumbs for location-only pages
- [ ] Add "All Categories" option to category dropdown on browse page

---

## Priority

| Feature | Priority | Reason |
|---------|----------|--------|
| Feature A: `all` pseudo-category | **P1** | Enables location-based SEO pages |
| Feature B: `q` text search | **P1** | Enables homepage search functionality |

Both features are needed for a complete search experience.

---

## Questions for Backend

1. Should "all" be case-insensitive? (Recommend: yes)
2. Any caching implications for the "all" category (larger result sets)?
3. Should attribute filters work when category is "all"? (Recommend: yes, for attributes that apply to multiple types)
4. For text search, should we search description too or just title? (Recommend: title only for performance, description optional)
5. Minimum query length for `q` parameter? (Recommend: 2 characters)

---

**Document Created:** 2026-05-09  
**Status:** Pending backend implementation
