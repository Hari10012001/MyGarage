package com.mygarage.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class ServiceRecordRequest {

    @NotNull(message = "Service date is required")
    private LocalDate serviceDate;

    @NotBlank(message = "Service type is required")
    @Size(max = 100)
    private String serviceType;

    @Size(max = 500)
    private String description;

    @Size(max = 100)
    private String garageName;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal cost;

    @Min(value = 0, message = "Odometer cannot be negative")
    private Integer odometerAtService;

    private LocalDate nextServiceDueDate;

    @Size(max = 1000)
    private String notes;
}
