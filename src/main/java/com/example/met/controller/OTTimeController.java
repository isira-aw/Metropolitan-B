package com.example.met.controller;

import com.example.met.entity.OTtimeCalculator;
import com.example.met.service.OTTimeCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;

@RestController
@RequestMapping("/ot-time")
@RequiredArgsConstructor
@Slf4j
public class OTTimeController {

    private final OTTimeCalculatorService otTimeCalculatorService;

    @PostMapping("/end-session")
    public ResponseEntity<?> endSession(
            @RequestParam String employeeEmail,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime endTime,
            @RequestParam String endLocation) {

        try {
            log.info("End session request for employee: {} on date: {}", employeeEmail, date);

            OTtimeCalculator otRecord = otTimeCalculatorService.handleEndSession(
                    employeeEmail, date, endTime, endLocation);

            return ResponseEntity.ok().body("Session ended successfully. OT calculated: " +
                    "Morning OT: " + otRecord.getMorningOTtime() +
                    ", Evening OT: " + otRecord.getEveningOTtime());

        } catch (Exception e) {
            log.error("Error ending session for employee: {}", employeeEmail, e);
            return ResponseEntity.badRequest().body("Error ending session: " + e.getMessage());
        }
    }
}