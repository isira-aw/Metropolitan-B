package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
public class JobEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String generatorid;
    private String workstatuslog;
    private String jobid;
    private String location;

    @Column(name = "event_time")
    private LocalDateTime eventTime; // Used LocalDateTime instead of LocalDate and LocalTime combined

    private String eventType;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
}
