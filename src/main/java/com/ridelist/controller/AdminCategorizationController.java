package com.ridelist.controller;

import com.ridelist.dto.request.CreateMakeRequest;
import com.ridelist.dto.request.CreateModelYearRequest;
import com.ridelist.dto.request.CreateVehicleModelRequest;
import com.ridelist.dto.request.UpdateCategorizationRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.MakeResponse;
import com.ridelist.dto.response.ModelYearResponse;
import com.ridelist.dto.response.VehicleModelResponse;
import com.ridelist.service.CategorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categorization")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Categorization", description = "Admin endpoints for managing vehicle categorization hierarchy")
@SecurityRequirement(name = "bearerAuth")
public class AdminCategorizationController {

    private final CategorizationService categorizationService;

    // ==================== MAKE ENDPOINTS ====================

    @Operation(summary = "Create a new make")
    @PostMapping("/makes")
    public ResponseEntity<ApiResponse<MakeResponse>> createMake(
            @Valid @RequestBody CreateMakeRequest request) {
        MakeResponse response = categorizationService.createMake(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Make created successfully", response));
    }

    @Operation(summary = "Update an existing make")
    @PutMapping("/makes/{id}")
    public ResponseEntity<ApiResponse<MakeResponse>> updateMake(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategorizationRequest request) {
        MakeResponse response = categorizationService.updateMake(id, request);
        return ResponseEntity.ok(ApiResponse.success("Make updated successfully", response));
    }

    @Operation(summary = "Delete a make (cascades to models and years)")
    @DeleteMapping("/makes/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMake(@PathVariable UUID id) {
        categorizationService.deleteMake(id);
        return ResponseEntity.ok(ApiResponse.success("Make deleted successfully", null));
    }

    @Operation(summary = "Get all makes")
    @GetMapping("/makes")
    public ResponseEntity<ApiResponse<List<MakeResponse>>> getAllMakes() {
        List<MakeResponse> response = categorizationService.getAllMakes();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== VEHICLE MODEL ENDPOINTS ====================

    @Operation(summary = "Create a new vehicle model")
    @PostMapping("/models")
    public ResponseEntity<ApiResponse<VehicleModelResponse>> createVehicleModel(
            @Valid @RequestBody CreateVehicleModelRequest request) {
        VehicleModelResponse response = categorizationService.createVehicleModel(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vehicle model created successfully", response));
    }

    @Operation(summary = "Update an existing vehicle model")
    @PutMapping("/models/{id}")
    public ResponseEntity<ApiResponse<VehicleModelResponse>> updateVehicleModel(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategorizationRequest request) {
        VehicleModelResponse response = categorizationService.updateVehicleModel(id, request);
        return ResponseEntity.ok(ApiResponse.success("Vehicle model updated successfully", response));
    }

    @Operation(summary = "Delete a vehicle model (cascades to years)")
    @DeleteMapping("/models/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicleModel(@PathVariable UUID id) {
        categorizationService.deleteVehicleModel(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle model deleted successfully", null));
    }

    @Operation(summary = "Get vehicle models for a make")
    @GetMapping("/makes/{makeId}/models")
    public ResponseEntity<ApiResponse<List<VehicleModelResponse>>> getModelsByMake(
            @PathVariable UUID makeId) {
        List<VehicleModelResponse> response = categorizationService.getModelsByMake(makeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== MODEL YEAR ENDPOINTS ====================

    @Operation(summary = "Create a new model year")
    @PostMapping("/years")
    public ResponseEntity<ApiResponse<ModelYearResponse>> createModelYear(
            @Valid @RequestBody CreateModelYearRequest request) {
        ModelYearResponse response = categorizationService.createModelYear(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Model year created successfully", response));
    }

    @Operation(summary = "Update an existing model year")
    @PutMapping("/years/{id}")
    public ResponseEntity<ApiResponse<ModelYearResponse>> updateModelYear(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCategorizationRequest request) {
        ModelYearResponse response = categorizationService.updateModelYear(id, request);
        return ResponseEntity.ok(ApiResponse.success("Model year updated successfully", response));
    }

    @Operation(summary = "Delete a model year")
    @DeleteMapping("/years/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteModelYear(@PathVariable UUID id) {
        categorizationService.deleteModelYear(id);
        return ResponseEntity.ok(ApiResponse.success("Model year deleted successfully", null));
    }

    @Operation(summary = "Get model years for a vehicle model")
    @GetMapping("/models/{modelId}/years")
    public ResponseEntity<ApiResponse<List<ModelYearResponse>>> getYearsByModel(
            @PathVariable UUID modelId) {
        List<ModelYearResponse> response = categorizationService.getYearsByModel(modelId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
