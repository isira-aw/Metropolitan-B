package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "job_event_logs")
public class JobEventLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String generatorId;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false)
    private String location; // You can store IP address or actual location

    @Column(nullable = false)
    private String jobId;

    @Column(nullable = false)
    private String action; // e.g., "UPDATE", "CREATE", "DELETE"

    public JobEventLog() {}

    public JobEventLog(String name, String email, String generatorId,
                       LocalDateTime dateTime, String location, String jobId, String action) {
        this.name = name;
        this.email = email;
        this.generatorId = generatorId;
        this.dateTime = dateTime;
        this.location = location;
        this.jobId = jobId;
        this.action = action;
    }
}