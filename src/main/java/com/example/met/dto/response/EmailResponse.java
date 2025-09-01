package com.example.met.dto.response;

import com.example.met.entity.EmailRecord;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {

    private UUID id;
    private UUID jobCardId;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String message;
    private String sentBy;
    private EmailRecord.EmailStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String errorMessage;

    public static EmailResponse fromEntity(EmailRecord emailRecord) {
        return EmailResponse.builder()
                .id(emailRecord.getId())
                .jobCardId(emailRecord.getJobCardId())
                .recipientEmail(emailRecord.getRecipientEmail())
                .recipientName(emailRecord.getRecipientName())
                .subject(emailRecord.getSubject())
                .message(emailRecord.getMessage())
                .sentBy(emailRecord.getSentBy())
                .status(emailRecord.getStatus())
                .createdAt(emailRecord.getCreatedAt())
                .sentAt(emailRecord.getSentAt())
                .errorMessage(emailRecord.getErrorMessage())
                .build();
    }
}