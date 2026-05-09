package com.ridelist.service;

import com.ridelist.cache.InMemoryCache;
import com.ridelist.dto.response.LocationCount;
import com.ridelist.dto.response.LocationHubResponse;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.Axis;
import com.ridelist.model.ListingType;
import com.ridelist.model.State;
import com.ridelist.model.VehicleType;
import com.ridelist.repository.AxisRepository;
import com.ridelist.repository.ListingRepository;
import com.ridelist.repository.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationHubService {

    private final ListingRepository listingRepository;
    private final StateRepository stateRepository;
    private final AxisRepository axisRepository;
    private final InMemoryCache cache;

    private static final String CACHE_PREFIX = "locationHub:";

    public LocationHubResponse getLocationHub(
            String categoryPath,
            ListingType listingType,
            VehicleType vehicleType,
            String stateSlug,
            String axisSlug) {

        String cacheKey = buildCacheKey(categoryPath, stateSlug, axisSlug);

        return cache.get(cacheKey, () -> computeLocationHub(
                categoryPath, listingType, vehicleType, stateSlug, axisSlug));
    }

    private LocationHubResponse computeLocationHub(
            String categoryPath,
            ListingType listingType,
            VehicleType vehicleType,
            String stateSlug,
            String axisSlug) {

        log.debug("Computing location hub for category={}, state={}, axis={}",
                categoryPath, stateSlug, axisSlug);

        // For "all" category, omit the category path from URLs for cleaner SEO
        boolean isAllCategory = "all".equalsIgnoreCase(categoryPath);

        if (stateSlug == null) {
            List<LocationCount> stateCounts = listingRepository
                    .countActiveListingsByCategory(listingType, vehicleType)
                    .stream()
                    .map(lc -> lc.withUrl(buildLocationUrl(isAllCategory, categoryPath, lc.slug(), null, null)))
                    .toList();
            return new LocationHubResponse("states", stateCounts);
        }

        if (axisSlug == null) {
            State state = stateRepository.findBySlug(stateSlug)
                    .orElseThrow(() -> new ResourceNotFoundException("State","slug", stateSlug));

            List<LocationCount> axisCounts = listingRepository
                    .countActiveListingsByStateAndCategory(state.getId(), listingType, vehicleType)
                    .stream()
                    .map(lc -> lc.withUrl(buildLocationUrl(isAllCategory, categoryPath, stateSlug, lc.slug(), null)))
                    .toList();
            return new LocationHubResponse("axes", axisCounts);
        }

        Axis axis = axisRepository.findBySlugAndStateSlug(axisSlug, stateSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Axis", "slug", axisSlug));

        List<LocationCount> areaCounts = listingRepository
                .countActiveListingsByAxisAndCategory(axis.getId(), listingType, vehicleType)
                .stream()
                .map(lc -> lc.withUrl(buildLocationUrl(isAllCategory, categoryPath, stateSlug, axisSlug, lc.slug())))
                .toList();
        return new LocationHubResponse("areas", areaCounts);
    }

    private String buildLocationUrl(boolean isAllCategory, String categoryPath, String stateSlug, String axisSlug, String areaSlug) {
        StringBuilder url = new StringBuilder();

        // For "all" category, omit category path for cleaner URLs (e.g., /lagos instead of /all/lagos)
        if (!isAllCategory) {
            url.append("/").append(categoryPath);
        }

        if (stateSlug != null) {
            url.append("/").append(stateSlug);
        }
        if (axisSlug != null) {
            url.append("/").append(axisSlug);
        }
        if (areaSlug != null) {
            url.append("/").append(areaSlug);
        }

        return url.toString();
    }

    private String buildCacheKey(String categoryPath, String stateSlug, String axisSlug) {
        return CACHE_PREFIX + categoryPath + "-" +
                (stateSlug != null ? stateSlug : "all") + "-" +
                (axisSlug != null ? axisSlug : "all");
    }

    public void evictLocationHubCache() {
        log.info("Evicting location hub cache");
        cache.evictAll();
    }
}
