package com.ridelist.email;

import com.ridelist.config.AppEmailProperties;
import com.ridelist.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;
    private final AppEmailProperties emailProps;
    private final AppProperties appProps;

    public EmailMessage buildWelcomeEmail(String toEmail, String firstName) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("appName", "RideList");
        context.setVariable("loginUrl", appProps.getFrontend().getBaseUrl() + "/");
        context.setVariable("year", LocalDate.now().getYear());

        String html = templateEngine.process("emails/welcome", context);

        return EmailMessage.builder()
                .to(toEmail)
                .toName(firstName)
                .subject("Welcome to RideList!")
                .htmlBody(html)
                .textBody("Welcome to RideList, " + firstName + "! " +
                        "Start browsing motorcycles at " + appProps.getFrontend().getBaseUrl())
                .build();
    }

    public EmailMessage buildPasswordRecoveryEmail(String toEmail, String firstName, String resetToken) {
        String resetUrl = appProps.getFrontend().getBaseUrl() + "/reset-password?token=" + resetToken;

        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("expiryMinutes", 30);
        context.setVariable("appName", "RideList");
        context.setVariable("year", LocalDate.now().getYear());

        String html = templateEngine.process("emails/password-recovery", context);

        return EmailMessage.builder()
                .to(toEmail)
                .toName(firstName)
                .subject("Reset your RideList password")
                .htmlBody(html)
                .textBody("Hi " + firstName + ",\n\n" +
                        "Reset your password here: " + resetUrl + "\n\n" +
                        "This link expires in 30 minutes.\n\n" +
                        "If you did not request this, ignore this email.")
                .build();
    }
}
