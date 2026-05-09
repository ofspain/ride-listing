package com.ridelist.dto.response;

public record BrowsePageMeta(
        String title,
        String description,
        String canonicalUrl,
        String locationLabel,
        String categoryLabel
) {}
