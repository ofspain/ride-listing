package com.ridelist.dto.response;

import com.ridelist.model.ContactStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactRequestResponse {

    private UUID id;
    private ListingSummaryResponse listing;
    private UserSummaryResponse buyer;
    private String senderName;
    private String senderPhone;
    private String message;
    private ContactStatus status;
    private String sellerResponse;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
