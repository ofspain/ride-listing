# Listing SEO URL Architecture

## Pattern

```
/{category}/{state}/{axis}/{area}/{listing-number}-{title-slug}
```

## Examples

```
/motorcycles/lagos/mainland/ikeja/10247-honda-cb300r-tokunbo-2023
/tricycles/kano/central/10891-tvs-king-keke
/spare-parts/lagos/ikeja/10456-brake-pads
/bicycles/10500-mountain-bike-26-inch
```

## Resolution Strategy

Backend resolves by `listing_number` ONLY. The slug portion is cosmetic — ignored by the API. Changing the listing title updates the slug but old URLs still resolve via `listing_number`.

This approach provides:
- **Collision-proof uniqueness** — listing_number is the single source of truth
- **SEO-friendly URLs** — human-readable slugs for search engines
- **Backward compatibility** — old URLs with outdated slugs still work
- **Simplicity** — no slug collision handling or redirect logic needed

## listing_number

- PostgreSQL SEQUENCE starting at 10000
- Never changes after creation (`updatable = false` in JPA)
- Human-friendly reference: "Listing #10247"
- 5-digit minimum length looks professional from the start

**Database column:**
```sql
listing_number INTEGER UNIQUE NOT NULL DEFAULT nextval('listing_number_seq')
```

## slug

- Generated from title only using `SlugUtil.toListingSlug()`
- Truncated to 80 characters at word boundary
- Regenerated when title is updated
- Unicode normalized (ọ→o, é→e)
- NOT unique — uniqueness comes from listing_number prefix

**Database column:**
```sql
slug VARCHAR(300) NOT NULL
```

## Category Paths

| Path | ListingType | VehicleType |
|------|-------------|-------------|
| `motorcycles` | VEHICLE | MOTORCYCLE |
| `tricycles` | VEHICLE | TRICYCLE |
| `bicycles` | VEHICLE | BICYCLE |
| `vehicles` | VEHICLE | null |
| `spare-parts` | PART | (any) |

## URL Building

Use `SlugUtil.toListingUrl()` to generate canonical URLs:

```java
String url = SlugUtil.toListingUrl(
    listing.getListingType(),
    listing.getVehicleType(),
    listing.getState() != null ? listing.getState().getSlug() : null,
    listing.getAxis() != null ? listing.getAxis().getSlug() : null,
    listing.getArea() != null ? listing.getArea().getSlug() : null,
    listing.getListingNumber(),
    listing.getTitle()
);
```

## URL Parsing

Use `SlugUtil.extractListingNumber()` to extract the listing number from a URL ref:

```java
// Input: "10247-honda-cb300r-tokunbo"
Integer listingNumber = SlugUtil.extractListingNumber(ref);
// Output: 10247

// Then query by listing_number
Listing listing = listingRepository.findByListingNumber(listingNumber);
```

## Slug Generation Rules

1. Convert to lowercase
2. Normalize unicode characters (NFD + strip combining marks)
3. Replace `&` with `and`
4. Replace non-alphanumeric characters with space
5. Replace multiple spaces/hyphens with single hyphen
6. Strip leading/trailing hyphens
7. Truncate to 80 characters at word boundary
8. Return `"listing"` if result is empty

## API Endpoints

### Listing Resolution

**GET /api/v1/listings/ref/{ref}**

Resolves a listing by its listing number. The slug portion after the number is ignored.

```
GET /api/v1/listings/ref/10247
GET /api/v1/listings/ref/10247-honda-cb300r
GET /api/v1/listings/ref/10247-anything-here
```

All three resolve to the same listing (listing_number = 10247).

**GET /api/v1/listings/{idOrRef}**

Backward-compatible endpoint that accepts either:
- UUID: `a3f4b2c1-...` → UUID lookup
- Listing number: `10247` → number lookup
- Number + slug: `10247-honda` → number lookup

### Browse Endpoints

**GET /api/v1/listings/browse/{categoryPath}**

Browse listings by category and optional location slugs.

| Parameter | Description |
|-----------|-------------|
| `categoryPath` | Category path (motorcycles, tricycles, bicycles, vehicles, spare-parts) |
| `stateSlug` | Optional state slug filter |
| `axisSlug` | Optional axis slug filter |
| `areaSlug` | Optional area slug filter |
| `minPrice` | Optional minimum price |
| `maxPrice` | Optional maximum price |
| `page` | Page number (default: 0) |
| `size` | Page size (default: 20) |

```
GET /api/v1/listings/browse/motorcycles
GET /api/v1/listings/browse/motorcycles?stateSlug=lagos
GET /api/v1/listings/browse/motorcycles?stateSlug=lagos&axisSlug=mainland
GET /api/v1/listings/browse/spare-parts?stateSlug=abuja
```

**GET /api/v1/listings/browse/{categoryPath}/meta**

Returns SEO metadata for browse pages.

```json
{
  "title": "Motorcycles for Sale in Lagos",
  "description": "Browse motorcycles for sale in Lagos on RideList...",
  "canonicalUrl": "/motorcycles/lagos",
  "locationLabel": "Lagos",
  "categoryLabel": "Motorcycles"
}
```

## Response Fields

Listing responses include these SEO-related fields:

| Field | Type | Description |
|-------|------|-------------|
| `listingNumber` | Integer | Human-friendly listing reference (e.g., 10247) |
| `slug` | String | URL-safe title slug |
| `canonicalUrl` | String | Full canonical URL path |
| `categoryPath` | String | Category path segment (e.g., "motorcycles") |
| `statePath` | String | Browse URL to state level (ListingResponse only) |
| `axisPath` | String | Browse URL to axis level (ListingResponse only) |
| `areaPath` | String | Browse URL to area level (ListingResponse only) |

### Sitemap Endpoint

**GET /api/v1/listings/sitemap-data**

Returns recent active listings for sitemap generation. Limited to 1000 entries.

```json
{
  "listings": [
    {
      "url": "/motorcycles/lagos/mainland/ikeja/10247-honda-cb300r",
      "lastModified": "2026-05-03T10:30:00"
    }
  ]
}
```

## Implementation Status

- [x] Database migration (listing_number, slug)
- [x] SlugUtil methods
- [x] Response DTOs updated
- [x] createListing generates slug
- [x] updateListing regenerates slug on title change
- [x] GET /listings/ref/{ref} endpoint
- [x] GET /listings/{idOrRef} backward compat
- [x] GET /listings/browse/{categoryPath}
- [x] GET /listings/browse/{categoryPath}/meta
- [x] GET /listings/sitemap-data
- [x] Admin + account responses include canonicalUrl

## Frontend Integration Notes

The frontend should:

1. **Use canonicalUrl from API responses** for all listing links — never build URLs manually on frontend
2. **Call /listings/browse/{category}** for category browse pages
3. **Call /listings/ref/{number}-{slug}** for listing detail pages
4. **Use /listings/browse/{category}/meta** for dynamic page titles and meta tags
5. **Poll /listings/sitemap-data** for dynamic sitemap generation

Example frontend routing:

```javascript
// Listing detail page
<Link href={listing.canonicalUrl}>View Listing</Link>

// Category browse
fetch(`/api/v1/listings/browse/motorcycles?stateSlug=lagos`)

// Page metadata
const meta = await fetch(`/api/v1/listings/browse/motorcycles/meta?stateSlug=lagos`)
document.title = meta.title
```

## Implementation Files

| File | Purpose |
|------|---------|
| `V12__listing_seo_fields.sql` | Flyway migration |
| `model/Listing.java` | Entity with listingNumber and slug fields |
| `util/SlugUtil.java` | Slug generation and URL building utilities |
| `util/SlugUtilTest.java` | Unit tests for SlugUtil |
| `controller/ListingController.java` | SEO endpoints (ref, browse, meta, sitemap) |
| `dto/response/BrowsePageMeta.java` | Browse page metadata DTO |
| `dto/response/LocationResolution.java` | Location slug resolution result |
| `dto/response/SitemapData.java` | Sitemap data wrapper |
| `dto/response/SitemapEntry.java` | Individual sitemap entry |
