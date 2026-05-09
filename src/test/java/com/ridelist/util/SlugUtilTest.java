package com.ridelist.util;

import com.ridelist.model.ListingType;
import com.ridelist.model.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugUtilTest {

    @Nested
    @DisplayName("toListingSlug()")
    class ToListingSlugTests {

        @Test
        @DisplayName("converts normal title to correct slug")
        void normalTitle_producesCorrectSlug() {
            String result = SlugUtil.toListingSlug("Honda CB300R Tokunbo 2023");
            assertThat(result).isEqualTo("honda-cb300r-tokunbo-2023");
        }

        @Test
        @DisplayName("strips special characters")
        void specialCharacters_areStripped() {
            String result = SlugUtil.toListingSlug("Honda CB300R (Clean!) - 2023");
            assertThat(result).isEqualTo("honda-cb300r-clean-2023");
        }

        @Test
        @DisplayName("normalizes Nigerian/unicode characters")
        void nigerianCharacters_areNormalized() {
            String result = SlugUtil.toListingSlug("Ọkada Ẹlẹ́kọ́ Ụzọ");
            assertThat(result).isEqualTo("okada-eleko-uzo");
        }

        @Test
        @DisplayName("replaces & with 'and'")
        void ampersand_replacedWithAnd() {
            String result = SlugUtil.toListingSlug("Parts & Accessories");
            assertThat(result).isEqualTo("parts-and-accessories");
        }

        @Test
        @DisplayName("converts multiple spaces to single hyphen")
        void multipleSpaces_becomeSingleHyphen() {
            String result = SlugUtil.toListingSlug("Honda   CB300R    2023");
            assertThat(result).isEqualTo("honda-cb300r-2023");
        }

        @Test
        @DisplayName("strips leading and trailing hyphens")
        void leadingTrailingHyphens_areStripped() {
            String result = SlugUtil.toListingSlug("  -Honda CB300R-  ");
            assertThat(result).isEqualTo("honda-cb300r");
        }

        @Test
        @DisplayName("truncates long title at word boundary")
        void longTitle_truncatedAtWordBoundary() {
            String longTitle = "This is an extremely long listing title that exceeds eighty characters and should be truncated properly";
            String result = SlugUtil.toListingSlug(longTitle);
            assertThat(result.length()).isLessThanOrEqualTo(80);
            assertThat(result).doesNotEndWith("-");
        }

        @Test
        @DisplayName("returns 'listing' for null input")
        void nullInput_returnsListing() {
            String result = SlugUtil.toListingSlug(null);
            assertThat(result).isEqualTo("listing");
        }

        @Test
        @DisplayName("returns 'listing' for blank input")
        void blankInput_returnsListing() {
            String result = SlugUtil.toListingSlug("   ");
            assertThat(result).isEqualTo("listing");
        }

        @Test
        @DisplayName("returns 'listing' for empty input")
        void emptyInput_returnsListing() {
            String result = SlugUtil.toListingSlug("");
            assertThat(result).isEqualTo("listing");
        }

        @Test
        @DisplayName("handles only special characters")
        void onlySpecialCharacters_returnsListing() {
            String result = SlugUtil.toListingSlug("!!!@@@###");
            assertThat(result).isEqualTo("listing");
        }
    }

    @Nested
    @DisplayName("extractListingNumber()")
    class ExtractListingNumberTests {

        @Test
        @DisplayName("extracts number from full ref")
        void fullRef_extractsNumber() {
            Integer result = SlugUtil.extractListingNumber("10247-honda-cb300r");
            assertThat(result).isEqualTo(10247);
        }

        @Test
        @DisplayName("extracts number-only ref")
        void numberOnly_extractsNumber() {
            Integer result = SlugUtil.extractListingNumber("10247");
            assertThat(result).isEqualTo(10247);
        }

        @Test
        @DisplayName("returns null for non-numeric prefix")
        void nonNumericPrefix_returnsNull() {
            Integer result = SlugUtil.extractListingNumber("abc-honda");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("returns null for null input")
        void nullInput_returnsNull() {
            Integer result = SlugUtil.extractListingNumber(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("returns null for empty input")
        void emptyInput_returnsNull() {
            Integer result = SlugUtil.extractListingNumber("");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("returns null for blank input")
        void blankInput_returnsNull() {
            Integer result = SlugUtil.extractListingNumber("   ");
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("toCategoryPath()")
    class ToCategoryPathTests {

        @Test
        @DisplayName("VEHICLE + MOTORCYCLE returns 'motorcycles'")
        void vehicleMotorcycle_returnsMotorcycles() {
            String result = SlugUtil.toCategoryPath(ListingType.VEHICLE, VehicleType.MOTORCYCLE);
            assertThat(result).isEqualTo("motorcycles");
        }

        @Test
        @DisplayName("VEHICLE + TRICYCLE returns 'tricycles'")
        void vehicleTricycle_returnsTricycles() {
            String result = SlugUtil.toCategoryPath(ListingType.VEHICLE, VehicleType.TRICYCLE);
            assertThat(result).isEqualTo("tricycles");
        }

        @Test
        @DisplayName("VEHICLE + BICYCLE returns 'bicycles'")
        void vehicleBicycle_returnsBicycles() {
            String result = SlugUtil.toCategoryPath(ListingType.VEHICLE, VehicleType.BICYCLE);
            assertThat(result).isEqualTo("bicycles");
        }

        @Test
        @DisplayName("VEHICLE + null returns 'vehicles'")
        void vehicleNull_returnsVehicles() {
            String result = SlugUtil.toCategoryPath(ListingType.VEHICLE, null);
            assertThat(result).isEqualTo("vehicles");
        }

        @Test
        @DisplayName("PART + null returns 'spare-parts'")
        void partNull_returnsSpareParts() {
            String result = SlugUtil.toCategoryPath(ListingType.PART, null);
            assertThat(result).isEqualTo("spare-parts");
        }

        @Test
        @DisplayName("PART + any vehicle type returns 'spare-parts'")
        void partWithVehicleType_returnsSpareParts() {
            String result = SlugUtil.toCategoryPath(ListingType.PART, VehicleType.MOTORCYCLE);
            assertThat(result).isEqualTo("spare-parts");
        }
    }

    @Nested
    @DisplayName("toListingUrl()")
    class ToListingUrlTests {

        @Test
        @DisplayName("full location produces complete URL path")
        void fullLocation_producesCompletePath() {
            String result = SlugUtil.toListingUrl(
                    ListingType.VEHICLE,
                    VehicleType.MOTORCYCLE,
                    "lagos",
                    "mainland",
                    "ikeja",
                    10247,
                    "Honda CB300R Tokunbo 2023"
            );
            assertThat(result).isEqualTo("/motorcycles/lagos/mainland/ikeja/10247-honda-cb300r-tokunbo-2023");
        }

        @Test
        @DisplayName("missing area produces path without area")
        void missingArea_producesPathWithoutArea() {
            String result = SlugUtil.toListingUrl(
                    ListingType.VEHICLE,
                    VehicleType.TRICYCLE,
                    "kano",
                    "central",
                    null,
                    10891,
                    "TVS King Keke"
            );
            assertThat(result).isEqualTo("/tricycles/kano/central/10891-tvs-king-keke");
        }

        @Test
        @DisplayName("missing axis and area produces path with only state")
        void missingAxisAndArea_producesPathWithOnlyState() {
            String result = SlugUtil.toListingUrl(
                    ListingType.VEHICLE,
                    VehicleType.BICYCLE,
                    "lagos",
                    null,
                    null,
                    10500,
                    "Mountain Bike"
            );
            assertThat(result).isEqualTo("/bicycles/lagos/10500-mountain-bike");
        }

        @Test
        @DisplayName("missing all location produces path without location")
        void missingAllLocation_producesPathWithoutLocation() {
            String result = SlugUtil.toListingUrl(
                    ListingType.VEHICLE,
                    VehicleType.MOTORCYCLE,
                    null,
                    null,
                    null,
                    10247,
                    "Honda CB300R"
            );
            assertThat(result).isEqualTo("/motorcycles/10247-honda-cb300r");
        }

        @Test
        @DisplayName("PART listing produces 'spare-parts' prefix")
        void partListing_producesSpareParts() {
            String result = SlugUtil.toListingUrl(
                    ListingType.PART,
                    null,
                    "lagos",
                    "ikeja",
                    null,
                    10456,
                    "Brake Pads Honda CB300R"
            );
            assertThat(result).isEqualTo("/spare-parts/lagos/ikeja/10456-brake-pads-honda-cb300r");
        }

        @Test
        @DisplayName("blank state is treated as missing")
        void blankState_treatedAsMissing() {
            String result = SlugUtil.toListingUrl(
                    ListingType.VEHICLE,
                    VehicleType.MOTORCYCLE,
                    "   ",
                    "mainland",
                    "ikeja",
                    10247,
                    "Honda CB300R"
            );
            assertThat(result).isEqualTo("/motorcycles/10247-honda-cb300r");
        }

        @Test
        @DisplayName("blank axis stops location chain")
        void blankAxis_stopsLocationChain() {
            String result = SlugUtil.toListingUrl(
                    ListingType.VEHICLE,
                    VehicleType.MOTORCYCLE,
                    "lagos",
                    "   ",
                    "ikeja",
                    10247,
                    "Honda CB300R"
            );
            assertThat(result).isEqualTo("/motorcycles/lagos/10247-honda-cb300r");
        }
    }

    @Nested
    @DisplayName("toListingRef()")
    class ToListingRefTests {

        @Test
        @DisplayName("combines listing number and slug")
        void combinesNumberAndSlug() {
            String result = SlugUtil.toListingRef(10247, "Honda CB300R Tokunbo 2023");
            assertThat(result).isEqualTo("10247-honda-cb300r-tokunbo-2023");
        }

        @Test
        @DisplayName("handles null title")
        void handlesNullTitle() {
            String result = SlugUtil.toListingRef(10247, null);
            assertThat(result).isEqualTo("10247-listing");
        }
    }

    @Nested
    @DisplayName("resolveCategoryPath()")
    class ResolveCategoryPathTests {

        @Test
        @DisplayName("'motorcycles' resolves to VEHICLE + MOTORCYCLE")
        void motorcycles_resolvesCorrectly() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath("motorcycles");
            assertThat(result).isNotNull();
            assertThat(result.listingType()).isEqualTo(ListingType.VEHICLE);
            assertThat(result.vehicleType()).isEqualTo(VehicleType.MOTORCYCLE);
        }

        @Test
        @DisplayName("'tricycles' resolves to VEHICLE + TRICYCLE")
        void tricycles_resolvesCorrectly() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath("tricycles");
            assertThat(result).isNotNull();
            assertThat(result.listingType()).isEqualTo(ListingType.VEHICLE);
            assertThat(result.vehicleType()).isEqualTo(VehicleType.TRICYCLE);
        }

        @Test
        @DisplayName("'bicycles' resolves to VEHICLE + BICYCLE")
        void bicycles_resolvesCorrectly() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath("bicycles");
            assertThat(result).isNotNull();
            assertThat(result.listingType()).isEqualTo(ListingType.VEHICLE);
            assertThat(result.vehicleType()).isEqualTo(VehicleType.BICYCLE);
        }

        @Test
        @DisplayName("'vehicles' resolves to VEHICLE + null")
        void vehicles_resolvesCorrectly() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath("vehicles");
            assertThat(result).isNotNull();
            assertThat(result.listingType()).isEqualTo(ListingType.VEHICLE);
            assertThat(result.vehicleType()).isNull();
        }

        @Test
        @DisplayName("'spare-parts' resolves to PART + null")
        void spareParts_resolvesCorrectly() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath("spare-parts");
            assertThat(result).isNotNull();
            assertThat(result.listingType()).isEqualTo(ListingType.PART);
            assertThat(result.vehicleType()).isNull();
        }

        @Test
        @DisplayName("'parts' resolves to PART + null (alias)")
        void parts_resolvesCorrectly() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath("parts");
            assertThat(result).isNotNull();
            assertThat(result.listingType()).isEqualTo(ListingType.PART);
            assertThat(result.vehicleType()).isNull();
        }

        @Test
        @DisplayName("'accessories' resolves to PART + null (alias)")
        void accessories_resolvesCorrectly() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath("accessories");
            assertThat(result).isNotNull();
            assertThat(result.listingType()).isEqualTo(ListingType.PART);
            assertThat(result.vehicleType()).isNull();
        }

        @Test
        @DisplayName("unknown category returns null")
        void unknownCategory_returnsNull() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath("cars");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("null category returns null")
        void nullCategory_returnsNull() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("blank category returns null")
        void blankCategory_returnsNull() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath("   ");
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("is case insensitive")
        void caseInsensitive() {
            SlugUtil.CategoryResolution result = SlugUtil.resolveCategoryPath("MOTORCYCLES");
            assertThat(result).isNotNull();
            assertThat(result.listingType()).isEqualTo(ListingType.VEHICLE);
            assertThat(result.vehicleType()).isEqualTo(VehicleType.MOTORCYCLE);
        }
    }
}
