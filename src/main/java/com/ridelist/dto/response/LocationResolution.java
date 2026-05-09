package com.ridelist.dto.response;

import java.util.UUID;

public record LocationResolution(
        UUID stateId,
        UUID axisId,
        UUID areaId
) {}
