package com.example.met.dto.response;

import com.example.met.enums.EmailStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EmailResponse {
    private UUID emailId;
    private UUID jobCardId;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String message;
    private LocalDateTime sentAt;
    private EmailStatus status;
}