package com.mygarage.dto.response;

public record PreTripChecklistItemDTO(
    String category,                    // MAINTENANCE, CONSUMABLE, FLUIDS, SAFETY, DOCUMENTATION
    String title,                       // Short actionable title
    String description,                 // Detailed guidance or reasoning
    String severity,                    // CRITICAL, ADVISORY, PASSED
    Boolean isActionRequired            // True if user must inspect or address before departure
) {}
