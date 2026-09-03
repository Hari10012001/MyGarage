package com.mygarage.model;

import com.mygarage.model.enums.FuelType;
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
 * FuelRecord entity - tracks every fuel fill-up.
 * Mileage is presented as ESTIMATED based on odometer difference.
 */
@Entity
@Table(name = "fuel_records")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class FuelRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fuel_id")
    private Long fuelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @NotNull(message = "Fuel date is required")
    @Column(name = "fuel_date", nullable = false)
    private LocalDate fuelDate;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Fuel type is required")
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.1", message = "Quantity must be greater than 0")
    @Digits(integer = 6, fraction = 2)
    @Column(name = "quantity_litres", nullable = false, precision = 8, scale = 2)
    private BigDecimal quantityLitres;

    @NotNull(message = "Cost per litre is required")
    @DecimalMin(value = "0.01", message = "Cost per litre must be greater than 0")
    @Digits(integer = 6, fraction = 2)
    @Column(name = "cost_per_litre", nullable = false, precision = 8, scale = 2)
    private BigDecimal costPerLitre;

    @Column(name = "total_cost", precision = 10, scale = 2)
    private BigDecimal totalCost;

    @Min(value = 0, message = "Odometer cannot be negative")
    @Column(name = "odometer_at_fill")
    private Integer odometerAtFill;

    /**
     * Estimated mileage (km/L) calculated from previous fuel record.
     * This is an estimate only - not guaranteed actual mileage.
     */
    @Column(name = "estimated_mileage_kmpl", precision = 6, scale = 2)
    private BigDecimal estimatedMileageKmpl;

    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        computeTotalCost();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        computeTotalCost();
    }

    private void computeTotalCost() {
        if (quantityLitres != null && costPerLitre != null) {
            this.totalCost = quantityLitres.multiply(costPerLitre);
        }
    }
}
