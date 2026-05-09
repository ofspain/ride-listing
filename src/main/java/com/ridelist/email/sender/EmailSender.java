package com.ridelist.email.sender;

import com.ridelist.email.EmailMessage;

public interface EmailSender {

    void send(EmailMessage message);

    String getSenderType();
}
