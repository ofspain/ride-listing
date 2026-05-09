package com.ridelist.dto.response;

import java.util.List;

public record SitemapData(
        List<SitemapEntry> listings
) {}
