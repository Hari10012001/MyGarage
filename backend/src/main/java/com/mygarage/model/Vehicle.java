package com.mygarage.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vehicle entity - owned by a User.
 * A user can own multiple vehicles.
 */
@Entity
@Table(name = "vehicles",
        uniqueConstraints = @UniqueConstraint(name = "uk_vehicle_user_plate", columnNames = {"user_id", "plate_number"}),
        indexes = {
            @Index(name = "idx_vehicles_user_id", columnList = "user_id"),
            @Index(name = "idx_vehicles_category_id", columnList = "category_id"),
            @Index(name = "idx_vehicles_plate", columnList = "plate_number")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long vehicleId;

    // Owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Category (e.g., Sedan, Bike)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private VehicleCategory category;

    @NotBlank(message = "Plate number is required")
    @Size(min = 4, max = 20, message = "Plate number must be 4-20 characters")
    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;

    @NotBlank(message = "Make is required")
    @Size(max = 50)
    @Column(name = "make", nullable = false, length = 50)
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 50)
    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Min(value = 1900, message = "Year must be after 1900")
    @Max(value = 2030, message = "Year must be realistic")
    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @Size(max = 30)
    @Column(name = "color", length = 30)
    private String color;

    @Size(max = 20)
    @Column(name = "fuel_type", length = 20)
    private String fuelType;

    @Min(value = 0, message = "Odometer cannot be negative")
    @Column(name = "current_odometer")
    private Integer currentOdometer = 0;

    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Child records - cascade delete when vehicle is removed
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("serviceDate DESC")
    private List<ServiceRecord> serviceRecords = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("fuelDate DESC")
    private List<FuelRecord> fuelRecords = new ArrayList<>();

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("scheduledDate ASC")
    private List<MaintenanceRecord> maintenanceRecords = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getDisplayName() {
        return make + " " + model + " (" + year + ")";
    }

    public void addServiceRecord(ServiceRecord record) {
        serviceRecords.add(record);
        record.setVehicle(this);
    }

    public void addFuelRecord(FuelRecord record) {
        fuelRecords.add(record);
        record.setVehicle(this);
    }

    public void addMaintenanceRecord(MaintenanceRecord record) {
        maintenanceRecords.add(record);
        record.setVehicle(this);
    }
}
