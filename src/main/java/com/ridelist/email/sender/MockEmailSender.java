package com.ridelist.email.sender;

import com.ridelist.email.EmailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MockEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        String bodyPreview = message.getHtmlBody()
                .substring(0, Math.min(200, message.getHtmlBody().length()))
                .replaceAll("<[^>]*>", "") + "...";

        log.info("""

                === MOCK EMAIL SENDER ===
                To: {} <{}>
                Subject: {}
                Body Preview: {}
                =========================
                """,
                message.getToName(),
                message.getTo(),
                message.getSubject(),
                bodyPreview);
    }

    @Override
    public String getSenderType() {
        return "mock";
    }
}
