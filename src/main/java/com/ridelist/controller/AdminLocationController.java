package com.ridelist.controller;

import com.ridelist.dto.request.CreateAreaRequest;
import com.ridelist.dto.request.CreateAxisRequest;
import com.ridelist.dto.request.CreateStateRequest;
import com.ridelist.dto.request.UpdateLocationRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AreaResponse;
import com.ridelist.dto.response.AxisResponse;
import com.ridelist.dto.response.StateResponse;
import com.ridelist.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/locations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLocationController {

    private final LocationService locationService;

    // ==================== STATE ENDPOINTS ====================

    @PostMapping("/states")
    public ResponseEntity<ApiResponse<StateResponse>> createState(
            @Valid @RequestBody CreateStateRequest request) {

        StateResponse response = locationService.createState(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("State created successfully", response));
    }

    @PutMapping("/states/{id}")
    public ResponseEntity<ApiResponse<StateResponse>> updateState(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLocationRequest request) {

        StateResponse response = locationService.updateState(id, request);
        return ResponseEntity.ok(ApiResponse.success("State updated successfully", response));
    }

    @DeleteMapping("/states/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteState(@PathVariable UUID id) {
        locationService.deleteState(id);
        return ResponseEntity.ok(ApiResponse.success("State deleted successfully", null));
    }

    @GetMapping("/states")
    public ResponseEntity<ApiResponse<List<StateResponse>>> getAllStates() {
        List<StateResponse> response = locationService.getAllStates();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== AXIS ENDPOINTS ====================

    @PostMapping("/axes")
    public ResponseEntity<ApiResponse<AxisResponse>> createAxis(
            @Valid @RequestBody CreateAxisRequest request) {

        AxisResponse response = locationService.createAxis(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Axis created successfully", response));
    }

    @PutMapping("/axes/{id}")
    public ResponseEntity<ApiResponse<AxisResponse>> updateAxis(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLocationRequest request) {

        AxisResponse response = locationService.updateAxis(id, request);
        return ResponseEntity.ok(ApiResponse.success("Axis updated successfully", response));
    }

    @DeleteMapping("/axes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAxis(@PathVariable UUID id) {
        locationService.deleteAxis(id);
        return ResponseEntity.ok(ApiResponse.success("Axis deleted successfully", null));
    }

    @GetMapping("/states/{stateId}/axes")
    public ResponseEntity<ApiResponse<List<AxisResponse>>> getAxesByState(
            @PathVariable UUID stateId) {

        List<AxisResponse> response = locationService.getAxesByState(stateId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== AREA ENDPOINTS ====================

    @PostMapping("/areas")
    public ResponseEntity<ApiResponse<AreaResponse>> createArea(
            @Valid @RequestBody CreateAreaRequest request) {

        AreaResponse response = locationService.createArea(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Area created successfully", response));
    }

    @PutMapping("/areas/{id}")
    public ResponseEntity<ApiResponse<AreaResponse>> updateArea(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLocationRequest request) {

        AreaResponse response = locationService.updateArea(id, request);
        return ResponseEntity.ok(ApiResponse.success("Area updated successfully", response));
    }

    @DeleteMapping("/areas/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArea(@PathVariable UUID id) {
        locationService.deleteArea(id);
        return ResponseEntity.ok(ApiResponse.success("Area deleted successfully", null));
    }

    @GetMapping("/axes/{axisId}/areas")
    public ResponseEntity<ApiResponse<List<AreaResponse>>> getAreasByAxis(
            @PathVariable UUID axisId) {

        List<AreaResponse> response = locationService.getAreasByAxis(axisId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
