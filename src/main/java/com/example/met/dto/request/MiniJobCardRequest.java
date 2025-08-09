package com.example.met.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
public class MiniJobCardRequest {
    @NotNull(message = "Job Card ID is required")
    private UUID jobCardId;

    @NotBlank(message = "Employee email is required")
    private String employeeEmail;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private String location;

    private LocalTime time;
}