package com.mygarage.service;

import com.mygarage.dto.response.GarageFuelIntelligenceDTO;
import com.mygarage.dto.response.VehicleFuelAnalyticsDTO;

public interface FuelAnalyticsService {
    
    /**
     * Get per-vehicle fuel analytics (M16)
     * @param vehicleId ID of the vehicle
     * @param userId ID of the logged in user (for ownership verification)
     * @return Complete analytics report
     */
    VehicleFuelAnalyticsDTO getVehicleFuelAnalytics(Long vehicleId, Long userId);

    /**
     * Get garage-level fuel intelligence (M16)
     * @param userId ID of the logged in user
     * @return Fleet-level analytics summary
     */
    GarageFuelIntelligenceDTO getGarageFuelIntelligence(Long userId);
}
