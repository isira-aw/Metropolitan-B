package com.example.met.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "log_id")
    private UUID logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_email", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    private String status;

    private String location;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @PrePersist
    private void setDefaults() {
        if (this.time == null) {
            this.time = LocalTime.now(java.time.ZoneId.of("Asia/Colombo"));
        }
        if (this.date == null) {
            this.date = LocalDate.now(java.time.ZoneId.of("Asia/Colombo"));
        }
        // Fix datetime nanosecond issue
        if (createdAt != null) {
            createdAt = createdAt.truncatedTo(ChronoUnit.MILLIS);
        }
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt != null ? createdAt.truncatedTo(ChronoUnit.MILLIS) : null;
    }
}