package com.example.met.dto.request;

import com.example.met.enums.JobCardType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobCardRequest {

    @NotNull(message = "Generator ID cannot be null")
    private UUID generatorId;

    @NotNull(message = "Job type cannot be null")
    private JobCardType jobType;

    @NotNull(message = "Date cannot be null")
    @FutureOrPresent(message = "Date cannot be in the past")
    private LocalDate date;

    private LocalTime estimatedTime;

    @NotNull(message = "Employee emails cannot be null")
    @NotEmpty(message = "At least one employee email is required")
    @Size(max = 20, message = "Cannot assign more than 20 employees")
    private List<@NotBlank(message = "Employee email cannot be blank")
    @Email(message = "Invalid email format") String> employeeEmails;
}