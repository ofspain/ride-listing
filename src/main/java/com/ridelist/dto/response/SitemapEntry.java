package com.ridelist.dto.response;

import java.time.LocalDateTime;

public record SitemapEntry(
        String url,
        LocalDateTime lastModified
) {}
