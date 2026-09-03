package com.mygarage.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter
public class MaintenanceRequest {

    @NotBlank(message = "Task title is required")
    @Size(max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    private LocalDate completedDate;

    @DecimalMin(value = "0.0", message = "Cost cannot be negative")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal cost;

    @Size(max = 1000)
    private String notes;
}
