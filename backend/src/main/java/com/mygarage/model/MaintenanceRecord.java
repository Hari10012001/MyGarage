package com.mygarage.model;

import com.mygarage.model.enums.MaintenanceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * MaintenanceRecord entity - tracks scheduled maintenance tasks.
 *
 * Status logic (computed at service layer, stored for persistence):
 *   completedDate != null         -> COMPLETED
 *   scheduledDate < today         -> OVERDUE
 *   scheduledDate == today        -> DUE_TODAY
 *   scheduledDate > today         -> UPCOMING
 */
@Entity
@Table(name = "maintenance_records",
        indexes = {
            @Index(name = "idx_maint_vehicle_id", columnList = "vehicle_id"),
            @Index(name = "idx_maint_status", columnList = "status"),
            @Index(name = "idx_maint_scheduled", columnList = "scheduled_date")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "maintenance_id")
    private Long maintenanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @NotBlank(message = "Task title is required")
    @Size(max = 100)
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Scheduled date is required")
    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MaintenanceStatus status = MaintenanceStatus.UPCOMING;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    @Size(max = 1000)
    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return completedDate != null;
    }
}
