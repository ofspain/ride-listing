package com.ridelist.email;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EmailMessage {
    private String to;
    private String toName;
    private String subject;
    private String htmlBody;
    private String textBody;
}
