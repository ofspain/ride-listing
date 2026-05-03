You are completing the SEO URL
implementation. Prompts 1, 2, 3 done.

Read before touching anything:
- SLUG-ARCHITECTURE.md
- All updated controller and service files
- AdminListingController.java
- dto/response/ListingSummaryResponse.java
- dto/response/ListingResponse.java

---

STEP 1 — Admin listing response update

AdminListingController returns listings
for the admin panel. Update admin
listing responses to also include
the canonicalUrl field so admins can
click through to the public SEO URL
from the admin panel.

Confirm ListingSummaryResponse already
includes canonicalUrl from Prompt 2.
If the admin panel uses a separate
AdminListingSummaryResponse DTO:
Add canonicalUrl there too.

---

STEP 2 — Account listings response

GET /account/listings returns the
dealer's own listings.
These responses must also include
canonicalUrl so the dealer workspace
can show the dealer their listing's
public URL.

Confirm the account endpoint uses
ListingSummaryResponse which already
has canonicalUrl.
If it uses a different DTO:
add canonicalUrl to that DTO.

---

STEP 3 — Sitemap data endpoint

The frontend needs a way to generate
a sitemap that includes real listing
URLs. Add a lightweight endpoint:

@GetMapping("/listings/sitemap-data")
public ApiResponse<SitemapData>
getSitemapData() {
// Return recently active listing
// canonical URLs for sitemap
// Limit to last 1000 active listings
// Only return canonicalUrl and
// updatedAt — nothing else

List<SitemapEntry> entries =
listingRepository
.findRecentActiveForSitemap(
PageRequest.of(0, 1000)
)
.stream()
.map(listing -> new SitemapEntry(
SlugUtil.toListingUrl(
listing.getListingType(),
listing.getVehicleType(),
listing.getState() != null
? listing.getState().getSlug()
: null,
listing.getAxis() != null
? listing.getAxis().getSlug()
: null,
listing.getArea() != null
? listing.getArea().getSlug()
: null,
listing.getListingNumber(),
listing.getTitle()
),
listing.getUpdatedAt()
))
.toList();

return ApiResponse.success(
new SitemapData(entries)
);
}

Add to ListingRepository:

@Query("SELECT l FROM Listing l " +
"WHERE l.status IN " +
"('ACTIVE', 'PUBLISHED') " +
"ORDER BY l.createdAt DESC")
List<Listing> findRecentActiveForSitemap(
Pageable pageable
);

Create records:
public record SitemapEntry(
String url,
LocalDateTime lastModified
) {}

public record SitemapData(
List<SitemapEntry> listings
) {}

Make this endpoint public in SecurityConfig:
/api/v1/listings/sitemap-data → public

---

STEP 4 — Log canonical URLs

In MarketplaceListingService update
logging to include listing_number:

Replace:
log.info("Listing created: {} by seller {}",
listingId, sellerId);

With:
log.info(
"Listing created: #{} ({}) by seller {}",
listing.getListingNumber(),
listing.getCanonicalUrl(),  
// if available in service
sellerId
);

This makes logs immediately linkable —
seeing #10247 in a log you can
construct the URL instantly.

---

STEP 5 — Final integration tests

1. Account listings response includes
   canonicalUrl
2. Admin listings response includes
   canonicalUrl
3. Sitemap data returns active listings
4. Sitemap entry urls match expected pattern
5. Sitemap limited to 1000 entries

---

AFTER ALL STEPS:

1. mvn test — all tests pass
2. Final refresh of docs/api-spec.json
3. Confirm all five new endpoints
   appear in Swagger UI
4. Manually test the full URL chain:
   a. Create a listing
   b. Note the listing_number in response
   c. Note the canonicalUrl in response
   d. Call GET /listings/ref/{number}
   e. Confirm returns correct listing
   f. Call GET /listings/browse/motorcycles
   ?stateSlug=lagos
   g. Confirm response contains listings
   with correct canonicalUrl format

Final update to SLUG-ARCHITECTURE.md:

Add section: ## Implementation Status
✅ Database migration (listing_number, slug)
✅ SlugUtil methods
✅ Response DTOs updated
✅ createListing generates slug
✅ updateListing regenerates slug on title change
✅ GET /listings/ref/{ref} endpoint
✅ GET /listings/{idOrRef} backward compat
✅ GET /listings/browse/{categoryPath}
✅ GET /listings/browse/{categoryPath}/meta
✅ GET /listings/sitemap-data
✅ Admin + account responses include canonicalUrl
✅ All tests passing

Add section: ## Frontend Integration Notes
The frontend should:
1. Use canonicalUrl from API responses
   for all listing links — never build
   URLs manually on frontend
2. Call /listings/browse/{category} for
   category browse pages
3. Call /listings/ref/{number}-{slug}
   for listing detail pages
4. Use /listings/browse/{category}/meta
   for dynamic page titles
5. Poll /listings/sitemap-data for
   dynamic sitemap generation

#### write test cases, but do not run the tests

#### UPDATE DOCS
update the SLUG-BASED-SEO-ARCHITECTURE.md documentation for this seo optimization and detail this feature there