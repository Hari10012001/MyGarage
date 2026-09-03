package com.mygarage.dto.request;

import com.mygarage.model.enums.FuelType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class FuelRecordRequest {

    @NotNull(message = "Fuel date is required")
    private LocalDate fuelDate;

    @NotNull(message = "Fuel type is required")
    private FuelType fuelType;

    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.1", message = "Quantity must be greater than 0")
    @Digits(integer = 6, fraction = 2)
    private BigDecimal quantityLitres;

    @NotNull(message = "Cost per litre is required")
    @DecimalMin(value = "0.01", message = "Cost per litre must be positive")
    @Digits(integer = 6, fraction = 2)
    private BigDecimal costPerLitre;

    @Min(value = 0, message = "Odometer cannot be negative")
    private Integer odometerAtFill;

    @Size(max = 500)
    private String notes;
}
