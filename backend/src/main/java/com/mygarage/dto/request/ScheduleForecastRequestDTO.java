package com.mygarage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleForecastRequestDTO {

    @NotBlank(message = "Task title is required")
    @Size(max = 150, message = "Title must not exceed 150 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Scheduled date is required")
    private LocalDate scheduledDate;

    @PositiveOrZero(message = "Estimated cost must be zero or positive")
    private BigDecimal estimatedCost;

    private Integer intervalKm;
}
