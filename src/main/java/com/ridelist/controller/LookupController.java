package com.ridelist.controller;

import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.SimpleNode;
import com.ridelist.service.CategorizationCacheService;
import com.ridelist.service.LocationCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Public lookup endpoints for reference data.
 * Returns cached data for frontend UI construction (dropdowns, filters, etc.).
 *
 * Covers:
 * - Location hierarchy: State → Axis → Area
 * - Vehicle categorization: Make → VehicleModel → ModelYear
 */
@RestController
@RequestMapping("/api/v1/lookup")
@RequiredArgsConstructor
@Tag(name = "Lookup", description = "Public endpoints for cached reference data (locations, categorization)")
public class LookupController {

    private final LocationCacheService locationCacheService;
    private final CategorizationCacheService categorizationCacheService;

    // ==================== LOCATION ENDPOINTS ====================

    @GetMapping("/states")
    @Operation(summary = "Get all states", description = "Returns cached list of all Nigerian states")
    public ResponseEntity<ApiResponse<List<SimpleNode>>> getStates() {
        List<SimpleNode> states = locationCacheService.getStates();
        return ResponseEntity.ok(ApiResponse.success(states));
    }

    @GetMapping("/states/{stateId}/axes")
    @Operation(summary = "Get axes by state", description = "Returns cached list of axes for a given state")
    public ResponseEntity<ApiResponse<List<SimpleNode>>> getAxesByState(@PathVariable UUID stateId) {
        List<SimpleNode> axes = locationCacheService.getAxesByState(stateId);
        return ResponseEntity.ok(ApiResponse.success(axes));
    }

    @GetMapping("/axes/{axisId}/areas")
    @Operation(summary = "Get areas by axis", description = "Returns cached list of areas for a given axis")
    public ResponseEntity<ApiResponse<List<SimpleNode>>> getAreasByAxis(@PathVariable UUID axisId) {
        List<SimpleNode> areas = locationCacheService.getAreasByAxis(axisId);
        return ResponseEntity.ok(ApiResponse.success(areas));
    }

    // ==================== CATEGORIZATION ENDPOINTS ====================

    @GetMapping("/makes")
    @Operation(summary = "Get all makes", description = "Returns cached list of all vehicle manufacturers")
    public ResponseEntity<ApiResponse<List<SimpleNode>>> getMakes() {
        List<SimpleNode> makes = categorizationCacheService.getMakes();
        return ResponseEntity.ok(ApiResponse.success(makes));
    }

    @GetMapping("/makes/{makeId}/models")
    @Operation(summary = "Get models by make", description = "Returns cached list of vehicle models for a given make")
    public ResponseEntity<ApiResponse<List<SimpleNode>>> getModelsByMake(@PathVariable UUID makeId) {
        List<SimpleNode> models = categorizationCacheService.getModelsByMake(makeId);
        return ResponseEntity.ok(ApiResponse.success(models));
    }

    @GetMapping("/models/{modelId}/years")
    @Operation(summary = "Get years by model", description = "Returns cached list of model years for a given vehicle model")
    public ResponseEntity<ApiResponse<List<SimpleNode>>> getYearsByModel(@PathVariable UUID modelId) {
        List<SimpleNode> years = categorizationCacheService.getYearsByModel(modelId);
        return ResponseEntity.ok(ApiResponse.success(years));
    }
}
