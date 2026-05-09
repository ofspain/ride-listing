package com.ridelist.email.sender;

import com.ridelist.config.AppEmailProperties;
import com.ridelist.email.EmailMessage;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final AppEmailProperties emailProps;

    @Override
    public void send(EmailMessage message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emailProps.getFromAddress(), emailProps.getFromName());
            helper.setTo(message.getTo());
            helper.setSubject(message.getSubject());
            helper.setText(message.getTextBody(), message.getHtmlBody());

            mailSender.send(mimeMessage);

            log.info("Email sent via SMTP to: {}", message.getTo());

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", message.getTo(), e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public String getSenderType() {
        return "smtp";
    }
}
