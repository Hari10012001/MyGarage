package com.mygarage.service;

import com.mygarage.dto.request.VehicleRequest;
import com.mygarage.model.Vehicle;
import com.mygarage.model.VehicleCategory;
import com.mygarage.model.User;
import com.mygarage.repository.VehicleCategoryRepository;
import com.mygarage.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleCategoryRepository categoryRepository;
    private final UserService userService;

    public Vehicle addVehicle(Long userId, VehicleRequest request) {
        if (vehicleRepository.existsByPlateNumberIgnoreCaseAndUserUserId(
                request.getPlateNumber().trim(), userId)) {
            throw new IllegalArgumentException(
                    "A vehicle with plate number '" + request.getPlateNumber() + "' already exists in your garage.");
        }
        User user = userService.findById(userId);
        VehicleCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid vehicle category."));

        Vehicle vehicle = new Vehicle();
        vehicle.setUser(user);
        vehicle.setCategory(category);
        vehicle.setPlateNumber(request.getPlateNumber().trim().toUpperCase());
        vehicle.setMake(request.getMake().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setYear(request.getYear());
        vehicle.setColor(request.getColor());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setCurrentOdometer(request.getCurrentOdometer());
        vehicle.setNotes(request.getNotes());
        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesForUser(Long userId) {
        return vehicleRepository.findByUserUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicleForUser(Long vehicleId, Long userId) {
        return vehicleRepository.findByVehicleIdAndUserUserId(vehicleId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found or access denied."));
    }

    public Vehicle updateVehicle(Long vehicleId, Long userId, VehicleRequest request) {
        Vehicle vehicle = getVehicleForUser(vehicleId, userId);
        // Allow same plate if it belongs to this same vehicle
        String newPlate = request.getPlateNumber().trim().toUpperCase();
        if (!vehicle.getPlateNumber().equalsIgnoreCase(newPlate)) {
            if (vehicleRepository.existsByPlateNumberIgnoreCaseAndUserUserId(newPlate, userId)) {
                throw new IllegalArgumentException(
                        "A vehicle with plate number '" + newPlate + "' already exists in your garage.");
            }
        }
        VehicleCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid vehicle category."));

        vehicle.setCategory(category);
        vehicle.setPlateNumber(newPlate);
        vehicle.setMake(request.getMake().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setYear(request.getYear());
        vehicle.setColor(request.getColor());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setCurrentOdometer(request.getCurrentOdometer());
        vehicle.setNotes(request.getNotes());
        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(Long vehicleId, Long userId) {
        Vehicle vehicle = getVehicleForUser(vehicleId, userId);
        vehicleRepository.delete(vehicle);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> searchVehicles(Long userId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getVehiclesForUser(userId);
        }
        return vehicleRepository.searchByUserAndKeyword(userId, keyword.trim());
    }

    @Transactional(readOnly = true)
    public long countVehiclesForUser(Long userId) {
        return vehicleRepository.countByUserUserId(userId);
    }
}
