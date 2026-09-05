package com.mygarage.dto.response;

import java.util.List;

public record GarageFleetDispatchReportDTO(
    Double tripDistanceKm,
    Integer tripDays,
    String drivingRegime,
    Long optimalVehicleId,
    String optimalVehicleName,
    String dispatchRecommendationSummary,
    List<FleetDispatchCandidateDTO> candidates,
    Integer totalActiveVehiclesEvaluated,
    Integer missionReadyVehiclesCount,
    String analyticalDisclaimer
) {}
