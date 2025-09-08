package com.example.met.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
public class VisitJobCardRequest {

    @NotNull(message = "Generator ID is required")
    private UUID generatorId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private LocalTime estimatedTime;

    @NotEmpty(message = "At least one employee email is required")
    @Size(max = 20, message = "Maximum 20 employees can be assigned")
    private List<String> employeeEmails;
}