package com.ridelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight DTO for cached reference data (locations, categories).
 *
 * Used for:
 * - State, Axis, Area in location hierarchy
 * - Any other simple reference data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleNode {

    private UUID id;
    private String name;
    private String slug;
}
