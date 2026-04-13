package com.ridelist.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AreaResponse {

    private UUID id;
    private String name;
    private String slug;
    private AxisResponse axis;
}
