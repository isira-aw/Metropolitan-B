package com.example.met.dto.response;

import com.example.met.enums.EmailStatus;
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

    private UUID id; // Changed from emailId to id
    private UUID jobCardId;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String message;
    private String sentBy;
    private EmailStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
    private String errorMessage;
}