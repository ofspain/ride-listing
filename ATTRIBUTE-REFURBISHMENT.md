# Attribute System Refurbishment

This document describes the breaking changes made to the RideList dynamic attributes system in migration V10.

## Overview

The attribute system was refurbished to support:
1. **Multiple listing types per attribute** - An attribute can now apply to both VEHICLE and PART listings
2. **Icon URLs** - Attributes can have an icon for frontend display
3. **Acceptable values list** - Predefined choices that users must select from (no free text)

## Schema Changes

### Migration V10

**File:** `V10__attribute_refurbishment.sql`

#### 1. Multiple ListingTypes (Breaking Change)

**Before:**
```sql
-- attribute_definitions table had:
listing_type VARCHAR(50) NOT NULL
```

**After:**
```sql
-- New join table
CREATE TABLE attribute_listing_types (
    attribute_id UUID NOT NULL REFERENCES attribute_definitions(id) ON DELETE CASCADE,
    listing_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (attribute_id, listing_type)
);

-- Old column dropped from attribute_definitions
```

#### 2. Icon URL (Non-breaking)

```sql
ALTER TABLE attribute_definitions ADD COLUMN icon_url VARCHAR(500);
```

#### 3. Acceptable Values (Breaking Change)

```sql
CREATE TABLE attribute_acceptable_values (
    attribute_id UUID NOT NULL REFERENCES attribute_definitions(id) ON DELETE CASCADE,
    value VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL DEFAULT 0
);
```

## Entity Changes

### AttributeDefinition

```java
// REMOVED
@Column(name = "listing_type", nullable = false)
private ListingType listingType;

// ADDED
@ElementCollection
@CollectionTable(name = "attribute_listing_types", joinColumns = @JoinColumn(name = "attribute_id"))
@Enumerated(EnumType.STRING)
@Column(name = "listing_type")
private Set<ListingType> listingTypes = new HashSet<>();

@Column(name = "icon_url", length = 500)
private String iconUrl;

@ElementCollection
@CollectionTable(name = "attribute_acceptable_values", joinColumns = @JoinColumn(name = "attribute_id"))
@Column(name = "value", length = 100)
@OrderColumn(name = "display_order")
private List<String> acceptableValues = new ArrayList<>();
```

## API Breaking Changes

### Create Attribute Request

**Before:**
```json
{
  "name": "Engine Type",
  "listingType": "VEHICLE",
  "filterable": true,
  "required": false
}
```

**After:**
```json
{
  "name": "Engine Type",
  "listingTypes": ["VEHICLE"],
  "iconUrl": "https://cdn.ridelist.ng/icons/engine.svg",
  "acceptableValues": ["150cc", "200cc", "250cc", "350cc", "400cc+"],
  "filterable": true,
  "required": false
}
```

### Update Attribute Request

New fields available:
- `listingTypes` (Set<ListingType>) - Replaces entire set
- `iconUrl` (String) - Valid URL or null
- `acceptableValues` (List<String>) - Replaces entire list

### Attribute Response

**Before:**
```json
{
  "id": "uuid",
  "name": "Engine Type",
  "slug": "engine-type",
  "listingType": "VEHICLE",
  "filterable": true,
  "required": false,
  "active": true
}
```

**After:**
```json
{
  "id": "uuid",
  "name": "Engine Type",
  "slug": "engine-type",
  "listingTypes": ["VEHICLE"],
  "iconUrl": "https://cdn.ridelist.ng/icons/engine.svg",
  "acceptableValues": ["150cc", "200cc", "250cc", "350cc", "400cc+"],
  "filterable": true,
  "required": false,
  "active": true
}
```

## Validation Rules

### Create Attribute
- `name` - Required, max 100 characters
- `listingTypes` - Required, at least one type (VEHICLE or PART)
- `acceptableValues` - Required, at least one value
- `iconUrl` - Optional, must be valid URL format if provided

### Listing Save with Attributes

When saving attributes on a listing, the following validations apply:

1. **Listing Type Membership Check:**
   ```
   If the attribute's listingTypes does NOT contain the listing's listingType:
   → BadRequestException: "Attribute 'X' does not apply to Y listings"
   ```

2. **Acceptable Value Check:**
   ```
   If the submitted value is NOT in the attribute's acceptableValues list:
   → BadRequestException: "Invalid value 'X' for attribute 'Y'. Acceptable values: [...]"
   ```

### Filter Validation

When filtering listings by attribute values:
- If a filter value is not in any listing's attribute values (implicitly an invalid value), the filter returns no results for that attribute
- No error is thrown - the system silently returns unfiltered results for that attribute
- Debug logging notes when a filter is applied

## Migration Order

1. `V1__create_initial_schema.sql` - Users, listings, images, contact requests
2. `V2__add_account_fields_to_users.sql` - Account type on users
3. `V3__add_listing_type_fields.sql` - Listing type fields
4. `V4__update_contact_requests_for_guests.sql` - Guest inquiry support
5. `V5__create_favorites_table.sql` - Favorites
6. `V6__add_location_hierarchy.sql` - States, axes, areas
7. `V7__add_vehicle_categorization.sql` - Makes, models, years
8. `V8__add_dynamic_attributes.sql` - Original attribute system
9. `V9__add_deleted_at_to_users.sql` - Soft delete
10. **`V10__attribute_refurbishment.sql`** - This refurbishment

## Repository Query Changes

JPQL queries now use `MEMBER OF` for listing type checks:

```java
// Before
List<AttributeDefinition> findByListingType(ListingType listingType);

// After
@Query("SELECT a FROM AttributeDefinition a WHERE :listingType MEMBER OF a.listingTypes")
List<AttributeDefinition> findByListingType(@Param("listingType") ListingType listingType);
```

## Frontend Integration

### Building Filter Dropdowns

```javascript
// Fetch attributes for listing type
const response = await fetch('/api/v1/attributes/filterable?listingType=VEHICLE');
const attributes = response.data;

// Each attribute now has:
attributes.forEach(attr => {
  console.log(attr.name);              // "Engine Type"
  console.log(attr.slug);              // "engine-type"  
  console.log(attr.iconUrl);           // "https://cdn.../engine.svg"
  console.log(attr.acceptableValues);  // ["150cc", "200cc", ...]
  console.log(attr.listingTypes);      // ["VEHICLE"]
  
  // Build dropdown with acceptableValues
  attr.acceptableValues.forEach(value => {
    // Create <option value={value}>{value}</option>
  });
});
```

### Applying Filters

```javascript
// Filter by attribute value (use slug, lowercase)
const params = new URLSearchParams();
params.set('attr_engine-type', '150cc');
params.set('attr_fuel-type', 'petrol');

const listings = await fetch(`/api/v1/listings?${params}`);
```

## Test Coverage

New integration tests added in `AttributeValidationIntegrationTest.java`:

- `ATTR-VAL-001`: Create listing with valid attribute value succeeds
- `ATTR-VAL-002`: Create listing with invalid attribute value fails (400)
- `ATTR-VAL-003`: Create listing with wrong listing type attribute fails (400)
- `ATTR-VAL-004`: Attribute applicable to both types works on VEHICLE
- `ATTR-VAL-005`: Attribute applicable to both types works on PART
- `ATTR-VAL-006`: Update listing with invalid attribute value fails
- `ATTR-VAL-007`: Update listing with valid attribute value succeeds
- `ATTR-FILTER-001`: Filter by valid attribute value returns matches
- `ATTR-FILTER-002`: Filter by invalid value returns empty (no error)
- Public endpoint tests for `acceptableValues`, `iconUrl`, `listingTypes`

Updated tests in `AdminAttributeControllerIntegrationTest.java`:
- Create with multiple listing types
- Create with iconUrl
- Create with acceptableValues
- Update acceptableValues (replaces list)
- Update iconUrl
- Update listingTypes
- Validation for missing acceptableValues
- Validation for invalid iconUrl format
