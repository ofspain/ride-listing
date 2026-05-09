package com.ridelist.dto.response;

public record LocationCount(
        String name,
        String slug,
        String url,
        long count
) {
    public LocationCount(String name, String slug, long count) {
        this(name, slug, null, count);
    }

    public LocationCount withUrl(String url) {
        return new LocationCount(name, slug, url, count);
    }
}
