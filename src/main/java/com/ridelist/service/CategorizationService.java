package com.ridelist.service;

import com.ridelist.cache.InMemoryCache;
import com.ridelist.dto.mapper.CategorizationMapper;
import com.ridelist.dto.request.CreateMakeRequest;
import com.ridelist.dto.request.CreateModelYearRequest;
import com.ridelist.dto.request.CreateVehicleModelRequest;
import com.ridelist.dto.request.UpdateCategorizationRequest;
import com.ridelist.dto.response.MakeResponse;
import com.ridelist.dto.response.ModelYearResponse;
import com.ridelist.dto.response.VehicleModelResponse;
import com.ridelist.exception.DuplicateResourceException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.Make;
import com.ridelist.model.ModelYear;
import com.ridelist.model.VehicleModel;
import com.ridelist.repository.MakeRepository;
import com.ridelist.repository.ModelYearRepository;
import com.ridelist.repository.VehicleModelRepository;
import com.ridelist.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategorizationService {

    private final MakeRepository makeRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final ModelYearRepository modelYearRepository;
    private final CategorizationMapper categorizationMapper;
    private final InMemoryCache cache;

    // ==================== MAKE OPERATIONS ====================

    @Transactional
    public MakeResponse createMake(CreateMakeRequest request) {
        log.info("Creating make: {}", request.getName());

        String slug = SlugUtil.toSlug(request.getName());

        if (makeRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Make", "name", request.getName());
        }

        Make make = Make.builder()
                .name(request.getName().trim())
                .slug(slug)
                .build();

        Make savedMake = makeRepository.save(make);
        log.info("Created make with id: {}", savedMake.getId());

        cache.evictAll();
        return categorizationMapper.toMakeResponse(savedMake);
    }

    @Transactional
    public MakeResponse updateMake(UUID makeId, UpdateCategorizationRequest request) {
        log.info("Updating make: {}", makeId);

        Make make = makeRepository.findById(makeId)
                .orElseThrow(() -> new ResourceNotFoundException("Make", "id", makeId));

        String newSlug = SlugUtil.toSlug(request.getName());

        if (!make.getSlug().equals(newSlug) && makeRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("Make", "name", request.getName());
        }

        make.setName(request.getName().trim());
        make.setSlug(newSlug);

        Make updatedMake = makeRepository.save(make);
        log.info("Updated make: {}", makeId);

        cache.evictAll();
        return categorizationMapper.toMakeResponse(updatedMake);
    }

    @Transactional
    public void deleteMake(UUID makeId) {
        log.info("Deleting make: {}", makeId);

        Make make = makeRepository.findById(makeId)
                .orElseThrow(() -> new ResourceNotFoundException("Make", "id", makeId));

        makeRepository.delete(make);
        log.info("Deleted make: {}", makeId);

        cache.evictAll();
    }

    public List<MakeResponse> getAllMakes() {
        log.debug("Fetching all makes");
        return makeRepository.findAll().stream()
                .map(categorizationMapper::toMakeResponse)
                .toList();
    }

    // ==================== VEHICLE MODEL OPERATIONS ====================

    @Transactional
    public VehicleModelResponse createVehicleModel(CreateVehicleModelRequest request) {
        log.info("Creating vehicle model: {} for make: {}", request.getName(), request.getMakeId());

        Make make = makeRepository.findById(request.getMakeId())
                .orElseThrow(() -> new ResourceNotFoundException("Make", "id", request.getMakeId()));

        String slug = SlugUtil.toSlug(request.getName());

        if (vehicleModelRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("VehicleModel", "name", request.getName());
        }

        VehicleModel vehicleModel = VehicleModel.builder()
                .name(request.getName().trim())
                .slug(slug)
                .make(make)
                .build();

        VehicleModel savedModel = vehicleModelRepository.save(vehicleModel);
        log.info("Created vehicle model with id: {}", savedModel.getId());

        cache.evictAll();
        return categorizationMapper.toVehicleModelResponse(savedModel);
    }

    @Transactional
    public VehicleModelResponse updateVehicleModel(UUID modelId, UpdateCategorizationRequest request) {
        log.info("Updating vehicle model: {}", modelId);

        VehicleModel vehicleModel = vehicleModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleModel", "id", modelId));

        String newSlug = SlugUtil.toSlug(request.getName());

        if (!vehicleModel.getSlug().equals(newSlug) && vehicleModelRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("VehicleModel", "name", request.getName());
        }

        vehicleModel.setName(request.getName().trim());
        vehicleModel.setSlug(newSlug);

        VehicleModel updatedModel = vehicleModelRepository.save(vehicleModel);
        log.info("Updated vehicle model: {}", modelId);

        cache.evictAll();
        return categorizationMapper.toVehicleModelResponse(updatedModel);
    }

    @Transactional
    public void deleteVehicleModel(UUID modelId) {
        log.info("Deleting vehicle model: {}", modelId);

        VehicleModel vehicleModel = vehicleModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleModel", "id", modelId));

        vehicleModelRepository.delete(vehicleModel);
        log.info("Deleted vehicle model: {}", modelId);

        cache.evictAll();
    }

    public List<VehicleModelResponse> getModelsByMake(UUID makeId) {
        log.debug("Fetching vehicle models for make: {}", makeId);

        if (!makeRepository.existsById(makeId)) {
            throw new ResourceNotFoundException("Make", "id", makeId);
        }

        return vehicleModelRepository.findByMakeId(makeId).stream()
                .map(categorizationMapper::toVehicleModelResponse)
                .toList();
    }

    // ==================== MODEL YEAR OPERATIONS ====================

    @Transactional
    public ModelYearResponse createModelYear(CreateModelYearRequest request) {
        log.info("Creating model year: {} for vehicle model: {}", request.getName(), request.getVehicleModelId());

        VehicleModel vehicleModel = vehicleModelRepository.findById(request.getVehicleModelId())
                .orElseThrow(() -> new ResourceNotFoundException("VehicleModel", "id", request.getVehicleModelId()));

        String slug = SlugUtil.toSlug(request.getName());

        if (modelYearRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("ModelYear", "name", request.getName());
        }

        ModelYear modelYear = ModelYear.builder()
                .name(request.getName().trim())
                .slug(slug)
                .vehicleModel(vehicleModel)
                .build();

        ModelYear savedYear = modelYearRepository.save(modelYear);
        log.info("Created model year with id: {}", savedYear.getId());

        cache.evictAll();
        return categorizationMapper.toModelYearResponse(savedYear);
    }

    @Transactional
    public ModelYearResponse updateModelYear(UUID yearId, UpdateCategorizationRequest request) {
        log.info("Updating model year: {}", yearId);

        ModelYear modelYear = modelYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("ModelYear", "id", yearId));

        String newSlug = SlugUtil.toSlug(request.getName());

        if (!modelYear.getSlug().equals(newSlug) && modelYearRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("ModelYear", "name", request.getName());
        }

        modelYear.setName(request.getName().trim());
        modelYear.setSlug(newSlug);

        ModelYear updatedYear = modelYearRepository.save(modelYear);
        log.info("Updated model year: {}", yearId);

        cache.evictAll();
        return categorizationMapper.toModelYearResponse(updatedYear);
    }

    @Transactional
    public void deleteModelYear(UUID yearId) {
        log.info("Deleting model year: {}", yearId);

        ModelYear modelYear = modelYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("ModelYear", "id", yearId));

        modelYearRepository.delete(modelYear);
        log.info("Deleted model year: {}", yearId);

        cache.evictAll();
    }

    public List<ModelYearResponse> getYearsByModel(UUID modelId) {
        log.debug("Fetching model years for vehicle model: {}", modelId);

        if (!vehicleModelRepository.existsById(modelId)) {
            throw new ResourceNotFoundException("VehicleModel", "id", modelId);
        }

        return modelYearRepository.findByVehicleModelId(modelId).stream()
                .map(categorizationMapper::toModelYearResponse)
                .toList();
    }
}
