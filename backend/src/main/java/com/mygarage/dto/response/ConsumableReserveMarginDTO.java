package com.mygarage.dto.response;

public record ConsumableReserveMarginDTO(
    String subsystemName,               // e.g. Engine Oil & Filter, Braking System, Cooling System, Tires & Suspension
    Integer benchmarkIntervalKm,        // Model benchmark assumption (e.g. 10,000 km)
    Integer benchmarkIntervalDays,      // Model benchmark assumption (e.g. 180 days)
    Integer kmSinceLastService,         // Measured kilometers elapsed since last matching record
    Integer daysSinceLastService,       // Measured days elapsed since last matching record
    Integer remainingMarginKm,          // Distance margin before theoretical interval exhaustion
    Double remainingMarginPercent,      // Percentage of interval remaining (0.0 to 100.0)
    Integer postTripMarginKm,           // Remaining margin after the simulated trip
    Boolean willBreachMidTrip,          // True if the planned trip will exceed the interval
    Integer breachAtTripKm,             // Kilometer into the trip where the threshold is crossed
    String statusBand                   // OPTIMAL, ADEQUATE, LOW_RESERVE, BREACHED
) {}
