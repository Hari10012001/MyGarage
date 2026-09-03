package com.mygarage.model;

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
 * ServiceRecord entity - tracks every vehicle service visit.
 */
@Entity
@Table(name = "service_records",
        indexes = {
            @Index(name = "idx_service_vehicle_id", columnList = "vehicle_id"),
            @Index(name = "idx_service_date", columnList = "service_date")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ServiceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "service_id")
    private Long serviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @NotNull(message = "Service date is required")
    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @NotBlank(message = "Service type is required")
    @Size(max = 100)
    @Column(name = "service_type", nullable = false, length = 100)
    private String serviceType;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 100)
    @Column(name = "garage_name", length = 100)
    private String garageName;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    @Min(value = 0, message = "Odometer cannot be negative")
    @Column(name = "odometer_at_service")
    private Integer odometerAtService;

    @Column(name = "next_service_due_date")
    private LocalDate nextServiceDueDate;

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
}
