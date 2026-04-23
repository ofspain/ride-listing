You are a senior Spring Boot engineer, QA architect, and backend systems designer.

The RideList application implements a "Delete Account" feature using SOFT DELETE.

The implementation includes:

- User fields:
  - enabled (boolean)
  - deletedAt (timestamp)

- Global filtering rule:
  WHERE user.enabled = true AND user.deleted_at IS NULL

- Listings belong to users
- Listings from deleted users must not appear publicly
- Favorites are deleted when user deletes account
- Messages are retained (not deleted)
- Authentication must be blocked after deletion

---

# 🎯 OBJECTIVE

Design a comprehensive, production-grade integration test plan specifically for the "Delete Account" feature.

This plan must ensure:
- correctness
- data integrity
- security enforcement
- no leakage of deleted users or their data

This is NOT unit testing.

---

# ⚙️ TEST ENVIRONMENT

- Use real PostgreSQL (NO H2)
- Use Testcontainers for DB
- Use @SpringBootTest (full context)
- Mock external dependencies if needed

---

# 🧱 TEST COVERAGE

## 1. ACCOUNT DELETION FLOW

- Successfully delete account
- Verify:
  - enabled = false
  - deletedAt is set

- Attempt deleting already deleted account → expect error

---

## 2. AUTHENTICATION BEHAVIOR

- Deleted user cannot:
  - login
  - access protected endpoints

- Existing JWT (if present) should be rejected

---

## 3. GLOBAL FILTERING ENFORCEMENT (CRITICAL)

Verify that:

- Deleted users are NOT returned in:
  - user queries
  - listing queries
  - seller APIs

Explicitly test:
- user.enabled = false
- user.deletedAt != null

Ensure system behaves as if user does not exist

---

## 4. LISTING BEHAVIOR

- User creates listings
- Delete account

Verify:
- Listings are:
  - marked as DELETED or UNPUBLISHED
- Listings do NOT appear in:
  - public listing APIs
  - search results

---

## 5. FAVORITES

- User has favorites
- Delete account

Verify:
- Favorites are removed
- No orphan records remain

---

## 6. MESSAGES / INQUIRIES

- User has sent or received messages

Verify:
- Messages still exist after deletion
- Message integrity is preserved
- (Optional) sender info handling if anonymized

---

## 7. DATA INTEGRITY

- No foreign key violations
- No orphaned records:
  - listings
  - favorites
  - images

---

## 8. SECURITY

- User cannot delete another user’s account
- Only authenticated user can call DELETE /api/v1/account

---

## 9. EDGE CASES

- Delete user with:
  - no listings
  - many listings
  - no favorites
  - mixed data

- Concurrent deletion requests

---

## 10. CACHE IMPACT (IF CACHE EXISTS)

- After deletion:
  - cached data should not return deleted user data
  - cache should be evicted or refreshed

---

# 🧱 TEST STRUCTURE

Define:

- Test class naming conventions
- Suggested test classes:
  - AccountDeletionIntegrationTest
  - ListingVisibilityAfterDeletionTest
  - AuthAfterDeletionTest

- Use transactional tests where appropriate

---

# 🧪 TEST DATA STRATEGY

- Use builders or fixtures
- Avoid hardcoded IDs
- Create reusable setup methods:
  - createUser()
  - createListing(user)
  - createFavorite(user, listing)

---

# 📄 OUTPUT FORMAT

Produce a MARKDOWN document named:

DELETE_ACCOUNT_TEST_PLAN.md

Structure:

1. Overview
2. Test Environment Setup
3. Test Scenarios (grouped by feature)
4. Edge Cases
5. Data Integrity Checks
6. Security Validation
7. Risks & Gaps

---

# ⚠️ CONSTRAINTS

- Focus strictly on integration tests
- Do NOT include unit tests
- Do NOT include UI tests
- Keep it MVP-focused but robust

---

# 🎯 GOAL

The test plan should ensure that:

- deleted users are completely invisible in the system
- system integrity is preserved
- no unintended data exposure occurs

### Update context
Update the TEST_PLAN.md file of this project with this newly added test cases