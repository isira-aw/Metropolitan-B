package com.example.met.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportDataResponse {

    private LocalDate date;
    private String generatorNames;
    private String firstActionLocation;
    private String lastActionLocation;
    private Double fullWorkingTime;
    private Double morningOTTime;
    private Double eveningOTTime;
    private Double totalOTTime;

    // Calculated field
    public Double getTotalOTTime() {
        if (morningOTTime == null) morningOTTime = 0.0;
        if (eveningOTTime == null) eveningOTTime = 0.0;
        return morningOTTime + eveningOTTime;
    }
}
