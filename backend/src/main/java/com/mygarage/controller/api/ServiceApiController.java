package com.mygarage.controller.api;

import com.mygarage.config.AuthHelper;
import com.mygarage.dto.request.ServiceRecordRequest;
import com.mygarage.model.ServiceRecord;
import com.mygarage.service.ServiceRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ServiceApiController {

    private final ServiceRecordService serviceRecordService;
    private final AuthHelper authHelper;

    @GetMapping("/vehicles/{vehicleId}/services")
    public ResponseEntity<List<ServiceRecord>> getServices(
            @PathVariable Long vehicleId,
            @RequestParam(required = false) String search) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(serviceRecordService.searchServiceRecords(vehicleId, userId, search));
    }

    @PostMapping("/vehicles/{vehicleId}/services")
    public ResponseEntity<ServiceRecord> addService(@PathVariable Long vehicleId,
                                                     @Valid @RequestBody ServiceRecordRequest request) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceRecordService.addServiceRecord(vehicleId, userId, request));
    }

    @PutMapping("/services/{serviceId}")
    public ResponseEntity<ServiceRecord> updateService(@PathVariable Long serviceId,
                                                        @Valid @RequestBody ServiceRecordRequest request) {
        Long userId = authHelper.getCurrentUserId();
        return ResponseEntity.ok(serviceRecordService.updateServiceRecord(serviceId, userId, request));
    }

    @DeleteMapping("/services/{serviceId}")
    public ResponseEntity<Map<String, String>> deleteService(@PathVariable Long serviceId) {
        Long userId = authHelper.getCurrentUserId();
        serviceRecordService.deleteServiceRecord(serviceId, userId);
        return ResponseEntity.ok(Map.of("message", "Service record deleted."));
    }
}
