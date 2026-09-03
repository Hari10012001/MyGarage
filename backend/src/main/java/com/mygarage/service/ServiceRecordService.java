package com.mygarage.service;

import com.mygarage.dto.request.ServiceRecordRequest;
import com.mygarage.model.ServiceRecord;
import com.mygarage.model.Vehicle;
import com.mygarage.repository.ServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ServiceRecordService {

    private final ServiceRecordRepository serviceRecordRepository;
    private final VehicleService vehicleService;

    public ServiceRecord addServiceRecord(Long vehicleId, Long userId, ServiceRecordRequest request) {
        Vehicle vehicle = vehicleService.getVehicleForUser(vehicleId, userId);
        ServiceRecord record = new ServiceRecord();
        record.setVehicle(vehicle);
        record.setServiceDate(request.getServiceDate());
        record.setServiceType(request.getServiceType().trim());
        record.setDescription(request.getDescription());
        record.setGarageName(request.getGarageName());
        record.setCost(request.getCost());
        record.setOdometerAtService(request.getOdometerAtService());
        record.setNextServiceDueDate(request.getNextServiceDueDate());
        record.setNotes(request.getNotes());

        // Update vehicle odometer if higher
        if (request.getOdometerAtService() != null
                && request.getOdometerAtService() > (vehicle.getCurrentOdometer() == null ? 0 : vehicle.getCurrentOdometer())) {
            vehicle.setCurrentOdometer(request.getOdometerAtService());
        }
        return serviceRecordRepository.save(record);
    }

    @Transactional(readOnly = true)
    public List<ServiceRecord> getServiceRecordsForVehicle(Long vehicleId, Long userId) {
        vehicleService.getVehicleForUser(vehicleId, userId); // ownership check
        return serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
    }

    @Transactional(readOnly = true)
    public ServiceRecord getServiceRecord(Long serviceId, Long userId) {
        return serviceRecordRepository.findByServiceIdAndVehicleUserUserId(serviceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Service record not found or access denied."));
    }

    @Transactional(readOnly = true)
    public List<ServiceRecord> searchServiceRecords(Long vehicleId, Long userId, String keyword) {
        vehicleService.getVehicleForUser(vehicleId, userId);
        if (keyword == null || keyword.isBlank()) {
            return serviceRecordRepository.findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
        }
        return serviceRecordRepository.searchByVehicleAndKeyword(vehicleId, keyword.trim());
    }

    @Transactional(readOnly = true)
    public ServiceRecord getServiceRecordForVehicle(Long serviceId, Long vehicleId, Long userId) {
        vehicleService.getVehicleForUser(vehicleId, userId); // verify vehicle ownership
        ServiceRecord record = getServiceRecord(serviceId, userId); // verify service record ownership
        if (!record.getVehicle().getVehicleId().equals(vehicleId)) {
            throw new IllegalArgumentException("Service record does not belong to the specified vehicle.");
        }
        return record;
    }

    public ServiceRecord updateServiceRecord(Long serviceId, Long userId, ServiceRecordRequest request) {
        ServiceRecord record = getServiceRecord(serviceId, userId);
        record.setServiceDate(request.getServiceDate());
        record.setServiceType(request.getServiceType().trim());
        record.setDescription(request.getDescription());
        record.setGarageName(request.getGarageName());
        record.setCost(request.getCost());
        record.setOdometerAtService(request.getOdometerAtService());
        record.setNextServiceDueDate(request.getNextServiceDueDate());
        record.setNotes(request.getNotes());
        if (request.getOdometerAtService() != null
                && request.getOdometerAtService() > (record.getVehicle().getCurrentOdometer() == null ? 0 : record.getVehicle().getCurrentOdometer())) {
            record.getVehicle().setCurrentOdometer(request.getOdometerAtService());
        }
        return serviceRecordRepository.save(record);
    }

    public ServiceRecord updateServiceRecordForVehicle(Long serviceId, Long vehicleId, Long userId, ServiceRecordRequest request) {
        ServiceRecord record = getServiceRecordForVehicle(serviceId, vehicleId, userId);
        record.setServiceDate(request.getServiceDate());
        record.setServiceType(request.getServiceType().trim());
        record.setDescription(request.getDescription());
        record.setGarageName(request.getGarageName());
        record.setCost(request.getCost());
        record.setOdometerAtService(request.getOdometerAtService());
        record.setNextServiceDueDate(request.getNextServiceDueDate());
        record.setNotes(request.getNotes());
        if (request.getOdometerAtService() != null
                && request.getOdometerAtService() > (record.getVehicle().getCurrentOdometer() == null ? 0 : record.getVehicle().getCurrentOdometer())) {
            record.getVehicle().setCurrentOdometer(request.getOdometerAtService());
        }
        return serviceRecordRepository.save(record);
    }

    public void deleteServiceRecord(Long serviceId, Long userId) {
        ServiceRecord record = getServiceRecord(serviceId, userId);
        serviceRecordRepository.delete(record);
    }

    public void deleteServiceRecordForVehicle(Long serviceId, Long vehicleId, Long userId) {
        ServiceRecord record = getServiceRecordForVehicle(serviceId, vehicleId, userId);
        serviceRecordRepository.delete(record);
    }

    @Transactional(readOnly = true)
    public List<ServiceRecord> getRecentForUser(Long userId, int limit) {
        return serviceRecordRepository.findRecentByUserId(userId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public long countForUser(Long userId) {
        return serviceRecordRepository.countByVehicleUserUserId(userId);
    }
}
