package com.example.met.dto.response;

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
public class LogResponse {
    private UUID logId;
    private String employeeEmail;
    private String employeeName;
    private String action;
    private LocalDate date;
    private LocalTime time;
    private String status;
    private String location;
    private LocalDateTime createdAt;
    private String generatorName;
}