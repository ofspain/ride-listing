package com.ridelist.service;

import com.ridelist.cache.InMemoryCache;
import com.ridelist.dto.mapper.LocationMapper;
import com.ridelist.dto.request.CreateAreaRequest;
import com.ridelist.dto.request.CreateAxisRequest;
import com.ridelist.dto.request.CreateStateRequest;
import com.ridelist.dto.request.UpdateLocationRequest;
import com.ridelist.dto.response.AreaResponse;
import com.ridelist.dto.response.AxisResponse;
import com.ridelist.dto.response.StateResponse;
import com.ridelist.exception.DuplicateResourceException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.Area;
import com.ridelist.model.Axis;
import com.ridelist.model.State;
import com.ridelist.repository.AreaRepository;
import com.ridelist.repository.AxisRepository;
import com.ridelist.repository.StateRepository;
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
public class LocationService {

    private final StateRepository stateRepository;
    private final AxisRepository axisRepository;
    private final AreaRepository areaRepository;
    private final LocationMapper locationMapper;
    private final InMemoryCache cache;

    // ==================== STATE OPERATIONS ====================

    @Transactional
    public StateResponse createState(CreateStateRequest request) {
        log.info("Creating state: {}", request.getName());

        String slug = SlugUtil.toSlug(request.getName());

        if (stateRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("State", "name", request.getName());
        }

        State state = State.builder()
                .name(request.getName().trim())
                .slug(slug)
                .build();

        State savedState = stateRepository.save(state);
        log.info("Created state with id: {}", savedState.getId());

        cache.evictAll();
        return locationMapper.toStateResponse(savedState);
    }

    @Transactional
    public StateResponse updateState(UUID stateId, UpdateLocationRequest request) {
        log.info("Updating state: {}", stateId);

        State state = stateRepository.findById(stateId)
                .orElseThrow(() -> new ResourceNotFoundException("State", "id", stateId));

        String newSlug = SlugUtil.toSlug(request.getName());

        // Check for duplicate only if slug is changing
        if (!state.getSlug().equals(newSlug) && stateRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("State", "name", request.getName());
        }

        state.setName(request.getName().trim());
        state.setSlug(newSlug);

        State updatedState = stateRepository.save(state);
        log.info("Updated state: {}", stateId);

        cache.evictAll();
        return locationMapper.toStateResponse(updatedState);
    }

    @Transactional
    public void deleteState(UUID stateId) {
        log.info("Deleting state: {}", stateId);

        State state = stateRepository.findById(stateId)
                .orElseThrow(() -> new ResourceNotFoundException("State", "id", stateId));

        stateRepository.delete(state);
        log.info("Deleted state: {}", stateId);

        cache.evictAll();
    }

    public List<StateResponse> getAllStates() {
        log.debug("Fetching all states");
        return stateRepository.findAll().stream()
                .map(locationMapper::toStateResponse)
                .toList();
    }

    // ==================== AXIS OPERATIONS ====================

    @Transactional
    public AxisResponse createAxis(CreateAxisRequest request) {
        log.info("Creating axis: {} for state: {}", request.getName(), request.getStateId());

        State state = stateRepository.findById(request.getStateId())
                .orElseThrow(() -> new ResourceNotFoundException("State", "id", request.getStateId()));

        String slug = SlugUtil.toSlug(request.getName());

        if (axisRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Axis", "name", request.getName());
        }

        Axis axis = Axis.builder()
                .name(request.getName().trim())
                .slug(slug)
                .state(state)
                .build();

        Axis savedAxis = axisRepository.save(axis);
        log.info("Created axis with id: {}", savedAxis.getId());

        cache.evictAll();
        return locationMapper.toAxisResponse(savedAxis);
    }

    @Transactional
    public AxisResponse updateAxis(UUID axisId, UpdateLocationRequest request) {
        log.info("Updating axis: {}", axisId);

        Axis axis = axisRepository.findById(axisId)
                .orElseThrow(() -> new ResourceNotFoundException("Axis", "id", axisId));

        String newSlug = SlugUtil.toSlug(request.getName());

        // Check for duplicate only if slug is changing
        if (!axis.getSlug().equals(newSlug) && axisRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("Axis", "name", request.getName());
        }

        axis.setName(request.getName().trim());
        axis.setSlug(newSlug);

        Axis updatedAxis = axisRepository.save(axis);
        log.info("Updated axis: {}", axisId);

        cache.evictAll();
        return locationMapper.toAxisResponse(updatedAxis);
    }

    @Transactional
    public void deleteAxis(UUID axisId) {
        log.info("Deleting axis: {}", axisId);

        Axis axis = axisRepository.findById(axisId)
                .orElseThrow(() -> new ResourceNotFoundException("Axis", "id", axisId));

        axisRepository.delete(axis);
        log.info("Deleted axis: {}", axisId);

        cache.evictAll();
    }

    public List<AxisResponse> getAxesByState(UUID stateId) {
        log.debug("Fetching axes for state: {}", stateId);

        if (!stateRepository.existsById(stateId)) {
            throw new ResourceNotFoundException("State", "id", stateId);
        }

        return axisRepository.findByStateId(stateId).stream()
                .map(locationMapper::toAxisResponse)
                .toList();
    }

    // ==================== AREA OPERATIONS ====================

    @Transactional
    public AreaResponse createArea(CreateAreaRequest request) {
        log.info("Creating area: {} for axis: {}", request.getName(), request.getAxisId());

        Axis axis = axisRepository.findById(request.getAxisId())
                .orElseThrow(() -> new ResourceNotFoundException("Axis", "id", request.getAxisId()));

        String slug = SlugUtil.toSlug(request.getName());

        if (areaRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Area", "name", request.getName());
        }

        Area area = Area.builder()
                .name(request.getName().trim())
                .slug(slug)
                .axis(axis)
                .build();

        Area savedArea = areaRepository.save(area);
        log.info("Created area with id: {}", savedArea.getId());

        cache.evictAll();
        return locationMapper.toAreaResponse(savedArea);
    }

    @Transactional
    public AreaResponse updateArea(UUID areaId, UpdateLocationRequest request) {
        log.info("Updating area: {}", areaId);

        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Area", "id", areaId));

        String newSlug = SlugUtil.toSlug(request.getName());

        // Check for duplicate only if slug is changing
        if (!area.getSlug().equals(newSlug) && areaRepository.existsBySlug(newSlug)) {
            throw new DuplicateResourceException("Area", "name", request.getName());
        }

        area.setName(request.getName().trim());
        area.setSlug(newSlug);

        Area updatedArea = areaRepository.save(area);
        log.info("Updated area: {}", areaId);

        cache.evictAll();
        return locationMapper.toAreaResponse(updatedArea);
    }

    @Transactional
    public void deleteArea(UUID areaId) {
        log.info("Deleting area: {}", areaId);

        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new ResourceNotFoundException("Area", "id", areaId));

        areaRepository.delete(area);
        log.info("Deleted area: {}", areaId);

        cache.evictAll();
    }

    public List<AreaResponse> getAreasByAxis(UUID axisId) {
        log.debug("Fetching areas for axis: {}", axisId);

        if (!axisRepository.existsById(axisId)) {
            throw new ResourceNotFoundException("Axis", "id", axisId);
        }

        return areaRepository.findByAxisId(axisId).stream()
                .map(locationMapper::toAreaResponse)
                .toList();
    }
}
