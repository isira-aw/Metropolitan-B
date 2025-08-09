package com.example.met.dto.response;

import com.example.met.enums.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniJobCardResponse {
    private UUID miniJobCardId;
    private UUID jobCardId;
    private String employeeEmail;
    private String employeeName;
    private JobStatus status;
    private LocalDate date;
    private String location;
    private LocalTime time;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}