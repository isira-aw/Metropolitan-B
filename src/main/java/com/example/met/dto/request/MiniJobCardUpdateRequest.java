package com.example.met.dto.request;

import com.example.met.enums.JobStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MiniJobCardUpdateRequest {
    private JobStatus status;
    private LocalDate date;
    private String location;
    private LocalTime time;
}