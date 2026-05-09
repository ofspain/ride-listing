package com.ridelist.dto.response;

import java.util.List;

public record LocationHubResponse(
        String level,
        List<LocationCount> locations
) {}
