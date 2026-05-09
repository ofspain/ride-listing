# Email Infrastructure

This document describes the email infrastructure for the RideList platform.

## Overview

The email system supports multiple sender implementations with easy switching via configuration. The architecture uses a factory pattern to select the active sender at runtime.

## Sender Types

| Type | Description | Usage |
|------|-------------|-------|
| `mock` | Logs emails to console without sending | Development, testing |
| `smtp` | Sends via SMTP (e.g., Gmail, SendGrid) | Staging, production |
| `ses` | Amazon SES (stub, not yet implemented) | Future production |

## How to Switch Senders

Set the `app.email.sender` property:

```properties
# application.properties
app.email.sender=mock    # Development (default)
app.email.sender=smtp    # Production with SMTP
app.email.sender=ses     # Future: Amazon SES
```

Or via environment variable:

```bash
EMAIL_SENDER=smtp java -jar ridelist.jar
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `EMAIL_SENDER` | `mock` | Active sender type |
| `EMAIL_FROM` | `noreply@ridelist.ng` | From email address |
| `EMAIL_FROM_NAME` | `RideList` | From display name |
| `FRONTEND_BASE_URL` | `http://localhost:8081` | Frontend URL for email links |
| `SMTP_HOST` | `smtp.gmail.com` | SMTP server host |
| `SMTP_PORT` | `587` | SMTP server port |
| `SMTP_USERNAME` | (empty) | SMTP username |
| `SMTP_PASSWORD` | (empty) | SMTP password/app password |

## Template Locations

Email templates are located at:

```
src/main/resources/templates/emails/
├── welcome.html           # Welcome email for new users
└── password-recovery.html # Password reset email
```

Templates use Thymeleaf with these common variables:
- `firstName` - User's first name
- `appName` - Application name ("RideList")
- `year` - Current year for copyright
- Template-specific variables (e.g., `resetUrl`, `loginUrl`)

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    EmailTemplateService                          │
│  - buildWelcomeEmail()                                          │
│  - buildPasswordRecoveryEmail()                                 │
└───────────────────────────────┬─────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                    EmailSenderFactory                            │
│  - getActiveSender() based on app.email.sender                  │
└───────────────────────────────┬─────────────────────────────────┘
                                │
              ┌─────────────────┼─────────────────┐
              ▼                 ▼                 ▼
┌───────────────────┐ ┌───────────────────┐ ┌───────────────────┐
│  MockEmailSender  │ │  SmtpEmailSender  │ │   SesEmailSender  │
│  (logs only)      │ │  (JavaMailSender) │ │   (stub)          │
└───────────────────┘ └───────────────────┘ └───────────────────┘
```

## Async Configuration

Email sending is configured for async execution to prevent blocking request threads:

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    // Thread pool: 2-5 threads, queue capacity 100
    // Thread name prefix: "email-async-"
}
```

## Adding a New Sender

To add a new email sender (e.g., Mailgun, Postmark):

1. Create a class implementing `EmailSender`:

```java
@Component
@Slf4j
public class MailgunEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        // Implementation here
    }

    @Override
    public String getSenderType() {
        return "mailgun";  // Used in app.email.sender
    }
}
```

2. The factory automatically discovers it via Spring injection.

3. Set `app.email.sender=mailgun` to activate.

No factory changes required - just implement the interface.

## Email Service Usage

```java
@Service
@RequiredArgsConstructor
public class SomeService {
    
    private final EmailTemplateService emailTemplateService;
    private final EmailSenderFactory emailSenderFactory;
    
    @Async
    public void sendWelcomeEmail(User user) {
        EmailMessage message = emailTemplateService
            .buildWelcomeEmail(user.getEmail(), user.getFirstName());
        
        emailSenderFactory.getActiveSender().send(message);
    }
}
```

## SMTP Configuration for Gmail

For Gmail SMTP:

1. Enable 2-Factor Authentication on your Google account
2. Generate an App Password: Google Account → Security → App passwords
3. Configure:

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
```

## Mock Sender Output

When using `mock` sender, emails are logged:

```
=== MOCK EMAIL SENDER ===
To: John Doe <john@example.com>
Subject: Welcome to RideList!
Body Preview: Hi John, Welcome to RideList - Nigeria's marketplace...
=========================
```

## Testing

The mock sender is ideal for integration tests - verify email sending without actual delivery.

For unit tests, mock the `EmailSenderFactory` and verify interactions.
