package com.mygarage.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VehicleRequest {

    @NotNull(message = "Category is required")
    private Long categoryId;

    @NotBlank(message = "Plate number is required")
    @Size(min = 4, max = 20, message = "Plate number must be 4-20 characters")
    private String plateNumber;

    @NotBlank(message = "Make is required")
    @Size(max = 50)
    private String make;

    @NotBlank(message = "Model is required")
    @Size(max = 50)
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be after 1900")
    @Max(value = 2030, message = "Year must be realistic")
    private Integer year;

    @Size(max = 30)
    private String color;

    @Size(max = 20)
    private String fuelType;

    @Min(value = 0, message = "Odometer cannot be negative")
    private Integer currentOdometer = 0;

    @Size(max = 500)
    private String notes;
}
