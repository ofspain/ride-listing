package com.ridelist.service;

import com.ridelist.cache.InMemoryCache;
import com.ridelist.dto.response.SimpleNode;
import com.ridelist.repository.AreaRepository;
import com.ridelist.repository.AxisRepository;
import com.ridelist.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service for accessing cached location data.
 *
 * Provides lazy-loaded, cached access to location hierarchy:
 * - States
 * - Axes (by state)
 * - Areas (by axis)
 *
 * Data is cached as SimpleNode DTOs to avoid caching JPA entities.
 * Cache is invalidated when admin modifies location data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LocationCacheService {

    private static final String CACHE_KEY_STATES = "states:all";
    private static final String CACHE_KEY_STATE_AXES = "state:%s:axes";
    private static final String CACHE_KEY_AXIS_AREAS = "axis:%s:areas";

    private final InMemoryCache cache;
    private final StateRepository stateRepository;
    private final AxisRepository axisRepository;
    private final AreaRepository areaRepository;

    /**
     * Get all states (cached).
     *
     * @return list of all states as SimpleNode
     */
    public List<SimpleNode> getStates() {
        return cache.get(CACHE_KEY_STATES, this::fetchAllStates);
    }

    /**
     * Get axes for a state (cached).
     *
     * @param stateId the state ID
     * @return list of axes as SimpleNode
     */
    public List<SimpleNode> getAxesByState(UUID stateId) {
        String cacheKey = String.format(CACHE_KEY_STATE_AXES, stateId);
        return cache.get(cacheKey, () -> fetchAxesByState(stateId));
    }

    /**
     * Get areas for an axis (cached).
     *
     * @param axisId the axis ID
     * @return list of areas as SimpleNode
     */
    public List<SimpleNode> getAreasByAxis(UUID axisId) {
        String cacheKey = String.format(CACHE_KEY_AXIS_AREAS, axisId);
        return cache.get(cacheKey, () -> fetchAreasByAxis(axisId));
    }

    /**
     * Invalidate all location cache entries.
     * Called when admin modifies location data.
     */
    public void invalidateAll() {
        cache.evictAll();
        log.info("Location cache invalidated");
    }

    // ==================== PRIVATE FETCH METHODS ====================

    private List<SimpleNode> fetchAllStates() {
        log.debug("Fetching all states from database");
        return stateRepository.findAll().stream()
                .map(state -> SimpleNode.builder()
                        .id(state.getId())
                        .name(state.getName())
                        .slug(state.getSlug())
                        .build())
                .toList();
    }

    private List<SimpleNode> fetchAxesByState(UUID stateId) {
        log.debug("Fetching axes for state {} from database", stateId);
        return axisRepository.findByStateId(stateId).stream()
                .map(axis -> SimpleNode.builder()
                        .id(axis.getId())
                        .name(axis.getName())
                        .slug(axis.getSlug())
                        .build())
                .toList();
    }

    private List<SimpleNode> fetchAreasByAxis(UUID axisId) {
        log.debug("Fetching areas for axis {} from database", axisId);
        return areaRepository.findByAxisId(axisId).stream()
                .map(area -> SimpleNode.builder()
                        .id(area.getId())
                        .name(area.getName())
                        .slug(area.getSlug())
                        .build())
                .toList();
    }
}
