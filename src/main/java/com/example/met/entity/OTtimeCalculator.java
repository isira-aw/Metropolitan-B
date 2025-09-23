package com.example.met.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ottimecalculator")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OTtimeCalculator {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ottimeid")
    private UUID otTimeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_email", nullable = false)
    private Employee employee;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime firsttime;

    @Column(nullable = false)
    private LocalTime lasttime;

    @Column(nullable = false)
    private LocalTime morningOTtime = LocalTime.of(0, 0, 0);

    @Column(nullable = false)
    private LocalTime eveningOTtime = LocalTime.of(0, 0, 0);

    @Column(name = "spent_on_ON_HOLD")
    private LocalTime spentOnOnHold = LocalTime.of(0, 0, 0);

    @Column(name = "spent_on_ASSIGNED")
    private LocalTime spentOnAssigned = LocalTime.of(0, 0, 0);

    @Column(name = "spent_on_IN_PROGRESS")
    private LocalTime spentOnInProgress = LocalTime.of(0, 0, 0);

    private String laststatus;
    private String currentstatus;

    private String firstLocation;
    private String lastLocation;

    // Store all locations visited during the day as JSON array
    @ElementCollection
    @CollectionTable(name = "ot_locations", joinColumns = @JoinColumn(name = "ot_time_id"))
    @Column(name = "location_list")
    private List<String> allLocations = new ArrayList<>();

    // Track when each status was last updated
    @Column(name = "status_change_time")
    private LocalDateTime statusChangeTime;

    @Column(name = "lastTime_update_ottime")
    private LocalDateTime lastTimeUpdateOTtime;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(SRI_LANKA_ZONE);
        this.createdAt = now;
        this.updatedAt = now;
        this.lastTimeUpdateOTtime = now;
        this.statusChangeTime = now;

        // Initialize collections
        if (this.allLocations == null) {
            this.allLocations = new ArrayList<>();
        }

        // Ensure required fields have defaults to prevent null constraint violations
        if (this.morningOTtime == null) {
            this.morningOTtime = LocalTime.of(0, 0, 0);
        }
        if (this.eveningOTtime == null) {
            this.eveningOTtime = LocalTime.of(0, 0, 0);
        }
        if (this.spentOnOnHold == null) {
            this.spentOnOnHold = LocalTime.of(0, 0, 0);
        }
        if (this.spentOnAssigned == null) {
            this.spentOnAssigned = LocalTime.of(0, 0, 0);
        }
        if (this.spentOnInProgress == null) {
            this.spentOnInProgress = LocalTime.of(0, 0, 0);
        }

        // CRITICAL: Ensure lasttime is never null
        if (this.lasttime == null && this.firsttime != null) {
            this.lasttime = this.firsttime; // Set to same as firsttime initially
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(SRI_LANKA_ZONE);
        this.lastTimeUpdateOTtime = LocalDateTime.now(SRI_LANKA_ZONE);
    }

    // Helper method to calculate total daily OT
    public LocalTime getTotalDailyOT() {
        if (morningOTtime == null || eveningOTtime == null) {
            return LocalTime.of(0, 0, 0);
        }

        int totalMinutes = (morningOTtime.getHour() * 60 + morningOTtime.getMinute()) +
                (eveningOTtime.getHour() * 60 + eveningOTtime.getMinute());

        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        // Handle overflow beyond 24 hours
        return LocalTime.of(hours % 24, minutes, 0);
    }

    // Helper method to update lasttime safely
    public void updateLastTime(LocalTime newTime) {
        if (newTime != null) {
            this.lasttime = newTime;
            this.lastTimeUpdateOTtime = LocalDateTime.now(SRI_LANKA_ZONE);
        }
    }

    // Helper method to set both first and last time for new entries
    public void setInitialTimes(LocalTime time) {
        if (time != null) {
            this.firsttime = time;
            this.lasttime = time; // Prevent null constraint violation
            this.lastTimeUpdateOTtime = LocalDateTime.now(SRI_LANKA_ZONE);
        }
    }

    // Helper method to add location to the list (avoiding duplicates if desired)
    public void addLocation(String location) {
        if (location != null && !location.trim().isEmpty()) {
            if (this.allLocations == null) {
                this.allLocations = new ArrayList<>();
            }

            // Add location (you can uncomment the next line if you want to avoid consecutive duplicates)
            // if (this.allLocations.isEmpty() || !this.allLocations.get(this.allLocations.size() - 1).equals(location.trim())) {
            this.allLocations.add(location.trim());
            // }

            // Update first and last location for backward compatibility
            if (this.firstLocation == null) {
                this.firstLocation = location.trim();
            }
            this.lastLocation = location.trim();
        }
    }

    // Helper method to get all unique locations
    public List<String> getUniqueLocations() {
        if (this.allLocations == null) {
            return new ArrayList<>();
        }
        return this.allLocations.stream().distinct().toList();
    }

    // Helper method to get locations as comma-separated string
    public String getLocationsAsString() {
        if (this.allLocations == null || this.allLocations.isEmpty()) {
            return "";
        }
        return String.join(", ", this.allLocations);
    }
}