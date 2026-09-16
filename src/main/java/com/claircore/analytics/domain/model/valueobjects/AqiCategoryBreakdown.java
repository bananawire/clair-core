package com.claircore.analytics.domain.model.valueobjects;

/**
 * Number of readings that fell into each AQI category over a period. Drives the
 * "% of time in category" figure (count / totalReadings) and the dominant
 * category. Stored as counts (not percentages) so monthly cascades can sum the
 * daily breakdowns exactly.
 */
public record AqiCategoryBreakdown(
        long good,
        long moderate,
        long unhealthyForSensitive,
        long unhealthy,
        long veryUnhealthy,
        long hazardous
) {
    public static AqiCategoryBreakdown empty() {
        return new AqiCategoryBreakdown(0, 0, 0, 0, 0, 0);
    }

    public long total() {
        return good + moderate + unhealthyForSensitive + unhealthy + veryUnhealthy + hazardous;
    }

    public long countOf(AqiCategory category) {
        return switch (category) {
            case GOOD -> good;
            case MODERATE -> moderate;
            case UNHEALTHY_FOR_SENSITIVE -> unhealthyForSensitive;
            case UNHEALTHY -> unhealthy;
            case VERY_UNHEALTHY -> veryUnhealthy;
            case HAZARDOUS -> hazardous;
        };
    }

    /** Category with the most readings; ties resolve to the more severe category. */
    public AqiCategory dominant() {
        AqiCategory dominant = AqiCategory.GOOD;
        long best = -1;
        for (AqiCategory c : AqiCategory.values()) {
            long count = countOf(c);
            if (count >= best) {
                best = count;
                dominant = c;
            }
        }
        return dominant;
    }

    public AqiCategoryBreakdown plus(AqiCategoryBreakdown other) {
        return new AqiCategoryBreakdown(
                good + other.good,
                moderate + other.moderate,
                unhealthyForSensitive + other.unhealthyForSensitive,
                unhealthy + other.unhealthy,
                veryUnhealthy + other.veryUnhealthy,
                hazardous + other.hazardous
        );
    }
}
