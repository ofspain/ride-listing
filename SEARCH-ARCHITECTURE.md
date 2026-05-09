# Search Architecture

This document describes the multi-value filter system for listing search.

## Multi-Value Filter Parameters

### Location Slugs

Filter by location using slug names (case-insensitive):

```
?location=ikeja&location=lekki
```

**Behavior:**
- Matches against `state.slug`, `axis.slug`, OR `area.slug`
- Multiple slugs combined with **OR** logic
- The frontend doesn't need to know the location hierarchy level
- Unknown slugs return empty results (no error)

**Examples:**
```
# Single location
GET /api/v1/listings?location=lagos

# Multiple locations (OR)
GET /api/v1/listings?location=ikeja&location=lekki

# State-level filter
GET /api/v1/listings?location=lagos

# Axis-level filter
GET /api/v1/listings?location=mainland

# Area-level filter
GET /api/v1/listings?location=opebi
```

### Attribute Filters

Filter by dynamic attributes using the `attr_` prefix:

```
?attr_{slug}=value&attr_{slug}=value2
```

**Behavior:**
- Multiple values for same attribute: **OR** logic
- Different attributes: **AND** logic
- Values are case-insensitive
- Unknown attribute slugs return empty results (no error)
- Invalid values return empty results (no error)

**Examples:**
```
# Single attribute value
GET /api/v1/listings?attr_engine-type=150cc

# Multiple values for same attribute (OR)
GET /api/v1/listings?attr_engine-type=150cc&attr_engine-type=200cc

# Multiple different attributes (AND)
GET /api/v1/listings?attr_engine-type=150cc&attr_fuel-type=petrol

# Combined multi-value
GET /api/v1/listings?attr_engine-type=150cc&attr_engine-type=200cc&attr_fuel-type=petrol
```

## Predicate Logic Summary

| Filter Type | Logic |
|-------------|-------|
| Multiple location slugs | OR |
| Multiple values for same attribute | OR |
| Different attribute types | AND |
| Location + attribute | AND |
| UUID filters (stateId, axisId, areaId) + slugs | AND |

## Combined Example

```
GET /api/v1/listings?location=ikeja&location=lekki&attr_engine-type=150cc&attr_engine-type=200cc&attr_fuel-type=petrol
```

Translates to:
```sql
WHERE 
  (state.slug IN ('ikeja', 'lekki') 
   OR axis.slug IN ('ikeja', 'lekki') 
   OR area.slug IN ('ikeja', 'lekki'))
  AND 
  EXISTS (SELECT 1 FROM listing_attribute_values 
          WHERE listing_id = l.id 
          AND attribute.slug = 'engine-type' 
          AND LOWER(value) IN ('150cc', '200cc'))
  AND 
  EXISTS (SELECT 1 FROM listing_attribute_values 
          WHERE listing_id = l.id 
          AND attribute.slug = 'fuel-type' 
          AND LOWER(value) = 'petrol')
```

## Backward Compatibility

The existing UUID-based filters remain functional:

| Parameter | Type | Description |
|-----------|------|-------------|
| `stateId` | UUID | Filter by state ID |
| `axisId` | UUID | Filter by axis ID |
| `areaId` | UUID | Filter by area ID |
| `listingType` | Enum | VEHICLE, PART |
| `vehicleType` | Enum | MOTORCYCLE, TRICYCLE, BICYCLE |
| `minPrice` | Decimal | Minimum price |
| `maxPrice` | Decimal | Maximum price |

These can be combined with the new slug-based filters.

## Endpoints

### Main Listing Endpoint

```
GET /api/v1/listings
```

Query parameters:
- `location` (repeatable) - Location slugs
- `attr_{slug}` (repeatable) - Attribute filters
- `stateId`, `axisId`, `areaId` - UUID location filters
- `listingType`, `vehicleType` - Type filters
- `minPrice`, `maxPrice` - Price range
- `page`, `size`, `sort` - Pagination

### Browse Endpoint (Query Params)

```
GET /api/v1/listings/browse/{categoryPath}
```

Supports all the same filters plus:
- `stateSlug`, `axisSlug`, `areaSlug` - SEO-friendly URL components

### Browse Endpoint (Path-Based SEO URLs)

```
GET /api/v1/listings/browse/{categoryPath}/{locationPath}
```

**This is the SEO-friendly endpoint** where location segments are part of the URL path.

**URL Pattern:**
```
/api/v1/listings/browse/{category}/{state-slug}/{axis-slug?}/{area-slug?}
```

**Examples:**
| URL | Resolves To |
|-----|-------------|
| `/api/v1/listings/browse/motorcycles/lagos` | State: Lagos |
| `/api/v1/listings/browse/motorcycles/lagos/ikeja` | State: Lagos, Axis: Ikeja |
| `/api/v1/listings/browse/motorcycles/lagos/ikeja/opebi` | State: Lagos, Axis: Ikeja, Area: Opebi |
| `/api/v1/listings/browse/spare-parts/abuja` | State: Abuja |

**Implementation Location:** `ListingController.java:browseListingsWithLocationPath()`

The `locationPath` is parsed by splitting on `/`:
```java
String[] locationSegments = locationPath.split("/");
String stateSlug = locationSegments.length > 0 ? locationSegments[0] : null;
String axisSlug = locationSegments.length > 1 ? locationSegments[1] : null;
String areaSlug = locationSegments.length > 2 ? locationSegments[2] : null;
```

Query parameters still supported:
- `location` (repeatable) - Additional location slugs for OR filtering
- `attr_{slug}` (repeatable) - Attribute filters
- `minPrice`, `maxPrice` - Price range
- `page`, `size`, `sort` - Pagination

## Graceful Degradation

The system handles edge cases gracefully:

| Scenario | Behavior |
|----------|----------|
| Unknown location slug | Returns empty results (no 500) |
| Unknown attribute slug | Returns empty results (no 500) |
| Invalid attribute value | Returns empty results (no 500) |
| Empty location list | No location filter applied |
| Empty attribute value list | No filter for that attribute |
| Inactive attribute | Ignored (no matches) |

## Implementation Files

| File | Purpose |
|------|---------|
| `repository/specification/ListingSpecification.java` | JPA Specification builder with multi-value support |
| `controller/ListingController.java` | Request parameter parsing |
| `service/MarketplaceListingService.java` | Wires specifications to repository |
| `repository/StateRepository.java` | `findBySlugIn()` for bulk lookups |
| `repository/AxisRepository.java` | `findBySlugIn()` for bulk lookups |
| `repository/AreaRepository.java` | `findBySlugIn()` for bulk lookups |

## "All Categories" Pseudo-Category

The `all` pseudo-category allows browsing **all listing types** (vehicles, parts) simultaneously.

### Usage

```
GET /api/v1/listings/browse/all
GET /api/v1/listings/browse/all/lagos
GET /api/v1/listings/browse/all/lagos/ikeja
GET /api/v1/listings/browse/all/lagos/ikeja/opebi
```

### Behavior

When `categoryPath = "all"`:
- `listingType` filter is **disabled** (returns VEHICLE + PART)
- `vehicleType` filter is **disabled** (returns MOTORCYCLE + TRICYCLE + BICYCLE)
- All other filters (location, price, attributes, search) work normally

### Implementation

**SlugUtil.java:**
```java
public static CategoryResolution resolveCategoryPath(String categoryPath) {
    return switch (categoryPath.toLowerCase()) {
        case "all" -> new CategoryResolution(null, null);  // null = no filter
        case "motorcycles" -> new CategoryResolution(ListingType.VEHICLE, VehicleType.MOTORCYCLE);
        // ...
    };
}
```

**ListingSpecification.java** skips type predicates when values are null:
```java
if (listingType != null) {
    predicates.add(criteriaBuilder.equal(root.get("listingType"), listingType));
}
if (vehicleType != null) {
    predicates.add(criteriaBuilder.equal(root.get("vehicleType"), vehicleType));
}
```

### SEO URL Generation

For SEO-friendly URLs, the "all" prefix is **omitted** from canonical URLs:

| API Path | Canonical URL |
|----------|---------------|
| `/browse/all/lagos` | `/lagos` |
| `/browse/all/lagos/ikeja` | `/lagos/ikeja` |
| `/browse/motorcycles/lagos` | `/motorcycles/lagos` |

The frontend maps location-only URLs (e.g., `/lagos/ikeja`) to `all` category API calls.

---

## Text Search (`q` Parameter)

The `q` parameter enables keyword search on listing titles.

### Usage

```
GET /api/v1/listings?q=honda
GET /api/v1/listings/browse/motorcycles?q=bajaj
GET /api/v1/listings/browse/all/lagos?q=brake
GET /api/v1/listings/browse/spare-parts/lagos/ikeja?q=helmet
```

### Behavior

| Aspect | Behavior |
|--------|----------|
| Field searched | `title` |
| Case sensitivity | Case-insensitive |
| Match type | Partial (LIKE `%query%`) |
| Minimum length | 2 characters |
| Empty/short `q` | No search filter applied |

### Implementation

**ListingSpecification.java:**
```java
if (searchQuery != null && searchQuery.length() >= 2) {
    String searchPattern = "%" + searchQuery.toLowerCase() + "%";
    predicates.add(criteriaBuilder.like(
            criteriaBuilder.lower(root.get("title")),
            searchPattern
    ));
}
```

### Combined with Other Filters

Text search can be combined with all other filters:

```
# Search + location slugs
GET /api/v1/listings?q=honda&location=ikeja&location=lekki

# Search + attributes
GET /api/v1/listings?q=honda&attr_engine-type=150cc

# Search + all category + location path
GET /api/v1/listings/browse/all/lagos/ikeja?q=honda

# Full combination
GET /api/v1/listings/browse/all/lagos?q=honda&location=ikeja&attr_engine-type=150cc&minPrice=100000
```

Translates to:
```sql
WHERE 
  LOWER(title) LIKE '%honda%'
  AND (state.slug = 'lagos' OR axis.slug = 'ikeja' OR area.slug = 'ikeja')
  AND EXISTS (... engine-type IN ('150cc') ...)
  AND price >= 100000
```

---

## Frontend Integration

### Building Filter URLs

```javascript
// Multi-location search
const locations = ['ikeja', 'lekki'];
const params = new URLSearchParams();
locations.forEach(loc => params.append('location', loc));

// Multi-value attribute
const engineTypes = ['150cc', '200cc'];
engineTypes.forEach(val => params.append('attr_engine-type', val));

// Combined
const url = `/api/v1/listings?${params.toString()}`;
// Result: /api/v1/listings?location=ikeja&location=lekki&attr_engine-type=150cc&attr_engine-type=200cc
```

### Filter State Management

```javascript
const filters = {
  query: 'honda',
  locations: ['ikeja', 'lekki'],
  attributes: {
    'engine-type': ['150cc', '200cc'],
    'fuel-type': ['petrol']
  }
};

function buildQueryParams(filters) {
  const params = new URLSearchParams();
  
  // Text search
  if (filters.query && filters.query.length >= 2) {
    params.set('q', filters.query);
  }
  
  // Location slugs
  filters.locations?.forEach(loc => params.append('location', loc));
  
  // Attribute filters
  Object.entries(filters.attributes || {}).forEach(([slug, values]) => {
    values.forEach(val => params.append(`attr_${slug}`, val));
  });
  
  return params.toString();
}
```

### Resolving Category from URL Path

```javascript
const CATEGORIES = ["motorcycles", "tricycles", "bicycles", "spare-parts", "vehicles", "all"];

function parseUrl(pathname) {
  const segments = pathname.split('/').filter(Boolean);
  
  if (segments.length === 0) {
    return { category: 'all', stateSlug: null, axisSlug: null, areaSlug: null };
  }
  
  // Check if first segment is a known category
  if (CATEGORIES.includes(segments[0])) {
    return {
      category: segments[0],
      stateSlug: segments[1] || null,
      axisSlug: segments[2] || null,
      areaSlug: segments[3] || null
    };
  }
  
  // Otherwise, treat as location-only URL (implicit "all" category)
  return {
    category: 'all',
    stateSlug: segments[0] || null,
    axisSlug: segments[1] || null,
    areaSlug: segments[2] || null
  };
}

// Examples:
// /motorcycles/lagos → { category: 'motorcycles', stateSlug: 'lagos', ... }
// /lagos/ikeja       → { category: 'all', stateSlug: 'lagos', axisSlug: 'ikeja', ... }
// /lagos             → { category: 'all', stateSlug: 'lagos', ... }
```

### Building API URLs

```javascript
function buildApiUrl(parsed, filters = {}) {
  const { category, stateSlug, axisSlug, areaSlug } = parsed;
  
  let path = `/api/v1/listings/browse/${category}`;
  
  if (stateSlug) {
    path += `/${stateSlug}`;
    if (axisSlug) {
      path += `/${axisSlug}`;
      if (areaSlug) {
        path += `/${areaSlug}`;
      }
    }
  }
  
  const params = buildQueryParams(filters);
  return params ? `${path}?${params}` : path;
}

// Example:
// buildApiUrl({ category: 'all', stateSlug: 'lagos' }, { query: 'honda' })
// → /api/v1/listings/browse/all/lagos?q=honda
```
