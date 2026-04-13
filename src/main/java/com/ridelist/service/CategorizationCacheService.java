package com.ridelist.service;

import com.ridelist.cache.InMemoryCache;
import com.ridelist.dto.response.SimpleNode;
import com.ridelist.repository.MakeRepository;
import com.ridelist.repository.ModelYearRepository;
import com.ridelist.repository.VehicleModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for accessing cached vehicle categorization data.
 *
 * Provides lazy-loaded, cached access to categorization hierarchy:
 * - Makes (all vehicle manufacturers)
 * - Vehicle Models (by make)
 * - Model Years (by vehicle model)
 *
 * Data is cached as SimpleNode DTOs to avoid caching JPA entities.
 * Cache is invalidated when admin modifies categorization data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategorizationCacheService {

    private static final String CACHE_KEY_MAKES = "makes:all";
    private static final String CACHE_KEY_MAKE_MODELS = "make:%s:models";
    private static final String CACHE_KEY_MODEL_YEARS = "model:%s:years";

    private final InMemoryCache cache;
    private final MakeRepository makeRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final ModelYearRepository modelYearRepository;

    /**
     * Get all makes (cached).
     *
     * @return list of all makes as SimpleNode
     */
    public List<SimpleNode> getMakes() {
        return cache.get(CACHE_KEY_MAKES, this::fetchAllMakes);
    }

    /**
     * Get vehicle models for a make (cached).
     *
     * @param makeId the make ID
     * @return list of vehicle models as SimpleNode
     */
    public List<SimpleNode> getModelsByMake(UUID makeId) {
        String cacheKey = String.format(CACHE_KEY_MAKE_MODELS, makeId);
        return cache.get(cacheKey, () -> fetchModelsByMake(makeId));
    }

    /**
     * Get model years for a vehicle model (cached).
     *
     * @param vehicleModelId the vehicle model ID
     * @return list of model years as SimpleNode
     */
    public List<SimpleNode> getYearsByModel(UUID vehicleModelId) {
        String cacheKey = String.format(CACHE_KEY_MODEL_YEARS, vehicleModelId);
        return cache.get(cacheKey, () -> fetchYearsByModel(vehicleModelId));
    }

    /**
     * Invalidate all categorization cache entries.
     * Called when admin modifies categorization data.
     */
    public void invalidateAll() {
        cache.evictAll();
        log.info("Categorization cache invalidated");
    }

    // ==================== PRIVATE FETCH METHODS ====================

    private List<SimpleNode> fetchAllMakes() {
        log.debug("Fetching all makes from database");
        return makeRepository.findAll().stream()
                .map(make -> SimpleNode.builder()
                        .id(make.getId())
                        .name(make.getName())
                        .slug(make.getSlug())
                        .build())
                .toList();
    }

    private List<SimpleNode> fetchModelsByMake(UUID makeId) {
        log.debug("Fetching vehicle models for make {} from database", makeId);
        return vehicleModelRepository.findByMakeId(makeId).stream()
                .map(model -> SimpleNode.builder()
                        .id(model.getId())
                        .name(model.getName())
                        .slug(model.getSlug())
                        .build())
                .toList();
    }

    private List<SimpleNode> fetchYearsByModel(UUID vehicleModelId) {
        log.debug("Fetching model years for vehicle model {} from database", vehicleModelId);
        return modelYearRepository.findByVehicleModelId(vehicleModelId).stream()
                .map(year -> SimpleNode.builder()
                        .id(year.getId())
                        .name(year.getName())
                        .slug(year.getSlug())
                        .build())
                .toList();
    }
}
