package com.ridelist.email.sender;

import com.ridelist.config.AppEmailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EmailSenderFactory {

    private final Map<String, EmailSender> senders;
    private final AppEmailProperties emailProps;

    public EmailSenderFactory(List<EmailSender> senderList, AppEmailProperties emailProps) {
        this.senders = senderList.stream()
                .collect(Collectors.toMap(
                        EmailSender::getSenderType,
                        Function.identity()
                ));
        this.emailProps = emailProps;

        log.info("EmailSenderFactory initialized with senders: {} | Active: {}",
                senders.keySet(),
                emailProps.getSender());
    }

    public EmailSender getActiveSender() {
        String senderType = emailProps.getSender();
        EmailSender sender = senders.get(senderType);

        if (sender == null) {
            log.warn("Unknown email sender type: {}. Falling back to mock.", senderType);
            return senders.get("mock");
        }

        return sender;
    }
}
