package com.mygarage.service;

import com.mygarage.model.VehicleCategory;
import com.mygarage.repository.VehicleCategoryRepository;
import com.mygarage.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class VehicleCategoryService {

    private final VehicleCategoryRepository categoryRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public List<VehicleCategory> findAll() {
        List<VehicleCategory> categories = categoryRepository.findAllByOrderByNameAsc();
        categories.forEach(c -> {
            if (c.getVehicles() != null) {
                c.getVehicles().size();
            }
        });
        return categories;
    }

    @Transactional(readOnly = true)
    public VehicleCategory findById(Long id) {
        VehicleCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        if (cat.getVehicles() != null) {
            cat.getVehicles().size();
        }
        return cat;
    }

    public VehicleCategory save(String name, String icon, String description) {
        if (categoryRepository.existsByNameIgnoreCase(name.trim())) {
            throw new IllegalArgumentException("A category named '" + name + "' already exists.");
        }
        VehicleCategory cat = new VehicleCategory();
        cat.setName(name.trim());
        cat.setIcon(icon != null && !icon.isBlank() ? icon.trim() : "🚗");
        cat.setDescription(description);
        return categoryRepository.save(cat);
    }

    public VehicleCategory update(Long id, String name, String icon, String description) {
        VehicleCategory cat = findById(id);
        if (!cat.getName().equalsIgnoreCase(name.trim())
                && categoryRepository.existsByNameIgnoreCase(name.trim())) {
            throw new IllegalArgumentException("A category named '" + name + "' already exists.");
        }
        cat.setName(name.trim());
        cat.setIcon(icon != null && !icon.isBlank() ? icon.trim() : "🚗");
        cat.setDescription(description);
        return categoryRepository.save(cat);
    }

    public void delete(Long id) {
        VehicleCategory cat = findById(id);
        long vehicleCount = vehicleRepository.countByCategoryCategoryId(id);
        if (vehicleCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete category '" + cat.getName() + "' — it has " + vehicleCount + " vehicle(s) assigned.");
        }
        categoryRepository.delete(cat);
    }
}
