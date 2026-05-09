# Password Recovery Flow

This document describes the password recovery implementation for the RideList platform.

## Overview

The password recovery system allows users to securely reset their forgotten passwords via email. The implementation prioritizes security and prevents user enumeration attacks.

## Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           PASSWORD RESET FLOW                                │
└─────────────────────────────────────────────────────────────────────────────┘

  User                    Frontend                  Backend                  Email
   │                         │                         │                       │
   │  1. Clicks "Forgot      │                         │                       │
   │     Password"           │                         │                       │
   │─────────────────────────►                         │                       │
   │                         │                         │                       │
   │                         │  2. POST /forgot-password                       │
   │                         │     { email }           │                       │
   │                         │─────────────────────────►                       │
   │                         │                         │                       │
   │                         │                         │  3. Generate token    │
   │                         │                         │     (32 bytes, Base64)│
   │                         │                         │                       │
   │                         │                         │  4. Save token        │
   │                         │                         │     (30 min expiry)   │
   │                         │                         │                       │
   │                         │                         │  5. Send reset email  │
   │                         │                         │─────────────────────► │
   │                         │                         │                       │
   │                         │  6. Always returns 200  │                       │
   │                         │     (no enumeration)    │                       │
   │                         │◄─────────────────────────                       │
   │                         │                         │                       │
   │  7. "Check your email"  │                         │                       │
   │◄─────────────────────────                         │                       │
   │                         │                         │                       │
   │  8. Clicks link in      │                         │                       │
   │     email               │                         │                       │
   │─────────────────────────►                         │                       │
   │                         │                         │                       │
   │                         │  9. POST /validate-reset-token                  │
   │                         │     { token }           │                       │
   │                         │─────────────────────────►                       │
   │                         │                         │                       │
   │                         │  10. 200 if valid       │                       │
   │                         │      400 if invalid     │                       │
   │                         │◄─────────────────────────                       │
   │                         │                         │                       │
   │  11. Shows reset form   │                         │                       │
   │      (if valid)         │                         │                       │
   │◄─────────────────────────                         │                       │
   │                         │                         │                       │
   │  12. Enters new         │                         │                       │
   │      password           │                         │                       │
   │─────────────────────────►                         │                       │
   │                         │                         │                       │
   │                         │  13. POST /reset-password                       │
   │                         │      { token,           │                       │
   │                         │        newPassword,     │                       │
   │                         │        confirmPassword }│                       │
   │                         │─────────────────────────►                       │
   │                         │                         │                       │
   │                         │                         │  14. Validate token   │
   │                         │                         │  15. Update password  │
   │                         │                         │  16. Mark token used  │
   │                         │                         │                       │
   │                         │  17. 200 success        │                       │
   │                         │◄─────────────────────────                       │
   │                         │                         │                       │
   │  18. "Password reset    │                         │                       │
   │      successfully"      │                         │                       │
   │◄─────────────────────────                         │                       │
   │                         │                         │                       │
```

## Endpoints

### POST /api/v1/auth/forgot-password

Request a password reset email.

**Request:**
```json
{
  "email": "user@example.com"
}
```

**Response (always 200):**
```json
{
  "success": true,
  "message": "If an account exists with this email, you will receive a password reset link shortly."
}
```

**Security:** Always returns 200 regardless of whether the email exists. This prevents attackers from determining which emails are registered.

### POST /api/v1/auth/validate-reset-token

Validate a password reset token before showing the reset form.

**Request:**
```json
{
  "token": "abc123..."
}
```

**Response (200 if valid):**
```json
{
  "success": true,
  "message": "Token is valid"
}
```

**Response (400 if invalid):**
```json
{
  "success": false,
  "message": "Invalid or expired reset link. Please request a new one."
}
```

### POST /api/v1/auth/reset-password

Reset the password using a valid token.

**Request:**
```json
{
  "token": "abc123...",
  "newPassword": "newSecurePassword123",
  "confirmPassword": "newSecurePassword123"
}
```

**Response (200 on success):**
```json
{
  "success": true,
  "message": "Password reset successfully. You can now log in with your new password."
}
```

**Response (400 on failure):**
```json
{
  "success": false,
  "message": "Passwords do not match"
}
```

## Token Security

| Property | Value | Reason |
|----------|-------|--------|
| Length | 32 bytes (Base64 encoded) | Cryptographically secure, unpredictable |
| Expiry | 30 minutes | Limits window of opportunity for attacks |
| One-time use | Yes | Token marked used after successful reset |
| Storage | Database | Allows invalidation and audit trail |
| Generation | `SecureRandom` | Cryptographically secure random number generator |

### Token Lifecycle

1. **Creation:** Generated when user requests password reset
2. **Validation:** Checked for existence, expiry, and used status
3. **Usage:** Marked as used immediately after successful password reset
4. **Cleanup:** Expired tokens deleted daily at 2 AM via scheduled task

### Security Measures

- **No user enumeration:** Same response for existing and non-existing emails
- **Rate limiting:** Should be implemented at infrastructure level (API gateway)
- **Token invalidation:** All previous tokens for a user are invalidated when a new one is requested
- **Async email sending:** Email failures don't expose user existence
- **BCrypt password hashing:** New passwords hashed with BCrypt before storage

## Email Templates

Password reset emails are rendered using Thymeleaf templates.

**Template location:** `src/main/resources/templates/emails/password-recovery.html`

**Template variables:**
| Variable | Description |
|----------|-------------|
| `firstName` | User's first name |
| `resetUrl` | Full URL with token (e.g., `https://ridelist.ng/reset-password?token=...`) |
| `expiryMinutes` | Token expiry time (30) |
| `year` | Current year for copyright |

## How to Switch Email Sender

Set the `app.email.sender` property:

```properties
# Development (default) - logs to console
app.email.sender=mock

# Production - sends via SMTP
app.email.sender=smtp

# Future - Amazon SES
app.email.sender=ses
```

Or via environment variable:
```bash
EMAIL_SENDER=smtp java -jar ridelist.jar
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `EMAIL_SENDER` | `mock` | Active sender type (`mock`, `smtp`, `ses`) |
| `EMAIL_FROM` | `noreply@ridelist.ng` | From email address |
| `EMAIL_FROM_NAME` | `RideList` | From display name |
| `FRONTEND_BASE_URL` | `http://localhost:8081` | Frontend URL for reset links |
| `SMTP_HOST` | `smtp.gmail.com` | SMTP server host |
| `SMTP_PORT` | `587` | SMTP server port |
| `SMTP_USERNAME` | (empty) | SMTP username |
| `SMTP_PASSWORD` | (empty) | SMTP password/app password |

## Database Schema

```sql
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_reset_tokens_user_id ON password_reset_tokens(user_id);
```

## Scheduled Cleanup

Expired tokens are automatically cleaned up daily at 2 AM:

```java
@Scheduled(cron = "0 0 2 * * ?")
@Transactional
public void cleanupExpiredTokens() {
    tokenRepository.deleteExpiredTokens(LocalDateTime.now());
}
```

## Frontend Integration

### Step 1: Forgot Password Page

```javascript
async function requestPasswordReset(email) {
  const response = await fetch('/api/v1/auth/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email })
  });
  
  // Always show success message (no enumeration)
  showMessage("If an account exists, you'll receive a reset link shortly.");
}
```

### Step 2: Reset Password Page

```javascript
// Extract token from URL: /reset-password?token=abc123
const token = new URLSearchParams(window.location.search).get('token');

// Validate token on page load
async function validateToken() {
  const response = await fetch('/api/v1/auth/validate-reset-token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token })
  });
  
  if (!response.ok) {
    // Show error and link to request new reset
    showError("This link is invalid or expired.");
    return false;
  }
  return true;
}

// Reset password
async function resetPassword(newPassword, confirmPassword) {
  const response = await fetch('/api/v1/auth/reset-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, newPassword, confirmPassword })
  });
  
  if (response.ok) {
    // Redirect to login
    window.location.href = '/login?reset=success';
  } else {
    const data = await response.json();
    showError(data.message);
  }
}
```

## Testing

The integration tests cover:

1. ✅ Forgot password with valid email → 200, token created
2. ✅ Forgot password with non-existent email → 200 (no enumeration)
3. ✅ Validate valid token → 200
4. ✅ Validate expired token → 400
5. ✅ Validate used token → 400
6. ✅ Reset password with valid token → 200, password changed
7. ✅ Reset password with mismatched passwords → 400
8. ✅ Reset password with expired token → 400
9. ✅ Reset password twice with same token → second fails
10. ✅ Welcome email sent on registration

Run tests:
```bash
mvn test -Dtest=PasswordRecoveryIntegrationTest
```
