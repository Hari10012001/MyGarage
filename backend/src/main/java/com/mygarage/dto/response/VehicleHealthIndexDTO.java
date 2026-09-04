package com.mygarage.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleHealthIndexDTO {

    private int healthScore;                  // Strictly clamped to 0 - 100
    private String healthRating;              // "EXCELLENT", "GOOD", "FAIR", "ATTENTION_REQUIRED"
    private String badgeColor;                // "success", "info", "warning", "danger"
    private int overdueTasksCount;            // Overdue maintenance tasks (-25 pts each)
    private int kmSinceLastService;           // Odometer difference since last recorded service
    private int serviceFreshnessScore;        // Component score (0 - 35 pts)
    private int maintenanceComplianceScore;   // Component score (0 - 45 pts)
    private int mileageStabilityScore;        // Component score (0 - 20 pts)
    private String recommendationSummary;     // Actionable guidance for user
}
