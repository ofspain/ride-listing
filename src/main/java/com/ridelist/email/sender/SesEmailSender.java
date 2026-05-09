package com.ridelist.email.sender;

import com.ridelist.email.EmailMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SesEmailSender implements EmailSender {

    @Override
    public void send(EmailMessage message) {
        log.warn(
                "SES sender not yet implemented. Email to {} was not sent. " +
                "Switch to smtp or mock sender.",
                message.getTo()
        );
        throw new UnsupportedOperationException("SES sender not yet implemented");
    }

    @Override
    public String getSenderType() {
        return "ses";
    }
}
