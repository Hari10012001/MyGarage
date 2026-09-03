package com.mygarage.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * VehicleCategory entity - managed by ADMIN.
 * Examples: Sedan, SUV, Motorcycle, Scooter, Truck, etc.
 */
@Entity
@Table(name = "vehicle_categories",
        uniqueConstraints = @UniqueConstraint(name = "uk_categories_name", columnNames = "name"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class VehicleCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 50, message = "Category name must be 2-50 characters")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Size(max = 10, message = "Icon must be max 10 characters")
    @Column(name = "icon", length = 10)
    private String icon = "🚗";

    @Column(name = "description", length = 200)
    private String description;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Vehicle> vehicles = new ArrayList<>();

    public long getVehicleCount() {
        return vehicles != null ? vehicles.size() : 0;
    }
}
