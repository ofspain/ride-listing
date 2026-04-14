You are a senior Spring Boot engineer, QA architect, and distributed systems engineer.

You are given a file `claude.md` which contains the full system design and feature set of a marketplace application called RideList.

Your task is to design a COMPREHENSIVE, production-grade integration test plan for the MVP.

---

# 🎯 OBJECTIVE

Produce a test plan that ensures the system is stable, correct, and resilient enough to support the first 100 active seller users.

This is NOT unit testing.

Focus on:
- End-to-end flows
- Real database behavior
- API-level correctness
- Data integrity
- Security enforcement

---

# ⚙️ TEST ENVIRONMENT REQUIREMENTS

- Use real PostgreSQL (DO NOT use H2)
- Use Testcontainers for PostgreSQL
- Provide application-test.yml configuration
- Use a separate test database

- Mock or stub AWS S3 interactions (do NOT call real S3)
- Use @SpringBootTest (full context)

---

# 🧱 TEST COVERAGE AREAS

## 1. AUTHENTICATION & SECURITY
- Register user
- Login
- Invalid login attempts
- JWT validation
- Access control:
 - USER endpoints
 - ADMIN endpoints
- Unauthorized access attempts

---

## 2. LISTING LIFECYCLE (CRITICAL PATH)
- Create listing (vehicle + part)
- Upload images
- Update listing
- Publish listing
- Mark as sold
- Delete listing

Validate:
- ownership enforcement
- invalid inputs
- missing fields

---

## 3. IMAGE UPLOAD (S3 INTEGRATION)
- Upload multiple images
- Reject invalid formats
- Reject oversized files
- Ensure metadata persistence
- Handle S3 failure gracefully (mocked)

---

## 4. LOCATION SYSTEM
- Admin creates State → Axis → Area
- Fetch hierarchy
- Validate relationships
- Invalid parent references

---

## 5. DYNAMIC ATTRIBUTE SYSTEM
- Admin creates attribute
- Seller assigns attributes to listing
- Retrieve listing with attributes
- Filter listings using attributes

---

## 6. FAVORITES
- Add to favorites
- Prevent duplicates
- Remove favorite
- Fetch user favorites

---

## 7. MESSAGING / INQUIRY
- Send inquiry
- Retrieve seller messages
- Validate message linkage to listing

---

## 8. CACHE BEHAVIOR
- First request hits DB
- Subsequent request uses cache
- Cache eviction after admin update

---

## 9. DATA INTEGRITY
- Cascading deletes (listing → images, favorites)
- Unique constraints enforcement
- Foreign key validation

---

## 10. SEARCH & FILTERING
- Filter by:
 - price range
 - state/axis/area
 - listingType
 - attributes
- Combine filters
- Empty result handling

---

# ⚡ PERFORMANCE SANITY (LIGHT)

Simulate:
- ~100 listings
- ~100 users

Test:
- listing retrieval performance
- filtering response time

(No need for full load testing — just sanity checks)

---

# 🧱 TEST STRUCTURE

Define:

- Test class naming conventions
- Package structure
- Use of:
 - @SpringBootTest
 - @Testcontainers
 - @Transactional (where appropriate)

---

# 🧪 TEST DATA STRATEGY

- Use builders or fixtures
- Avoid hardcoded IDs
- Reusable test data setup

---

# 📄 OUTPUT FORMAT

Produce a MARKDOWN document named:

TEST_PLAN.md

Structure:

1. Overview
2. Test Environment Setup
3. Test Strategy
4. Test Scenarios (grouped by feature)
5. Edge Cases
6. Performance Considerations
7. Risks & Gaps

---

# ⚠️ CONSTRAINTS

- Keep it MVP-focused (do NOT overengineer)
- Do NOT include unit tests
- Do NOT include UI tests
- Focus strictly on backend integration tests

---

# 🎯 GOAL

The output should be actionable by an engineer to implement tests immediately.