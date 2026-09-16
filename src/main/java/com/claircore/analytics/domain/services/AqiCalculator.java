package com.claircore.analytics.domain.services;

import com.claircore.analytics.domain.model.valueobjects.AirQualityIndex;
import com.claircore.analytics.domain.model.valueobjects.AqiCategory;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PM2.5-only index using EPA breakpoints revised in 2024 and decimal truncation.
 * The caller supplies the period's mean concentration. Live/hourly/monthly indices are
 * indicative PM indices, not a regulatory daily AQI or NowCast. CO2 remains a separate metric.
 * The product display is capped at 500; concentrations above the scale remain hazardous.
 */
public class AqiCalculator {
    private static final double[][] BANDS = {
            {0.0, 9.0, 0, 50}, {9.1, 35.4, 51, 100},
            {35.5, 55.4, 101, 150}, {55.5, 125.4, 151, 200},
            {125.5, 225.4, 201, 300}, {225.5, 325.4, 301, 500}
    };

    public AirQualityIndex calculateAqi(Double pm2_5) {
        if (pm2_5 == null || !Double.isFinite(pm2_5) || pm2_5 < 0)
            throw new IllegalArgumentException("PM2.5 must be finite and nonnegative; missing data has no index");
        if (pm2_5 >= 325.4) return index(500);
        double concentration = BigDecimal.valueOf(pm2_5).setScale(1, RoundingMode.DOWN).doubleValue();
        for (double[] b : BANDS) {
            if (concentration >= b[0] && concentration <= b[1]) {
                return index((int) Math.round((b[3] - b[2]) / (b[1] - b[0]) * (concentration - b[0]) + b[2]));
            }
        }
        throw new IllegalStateException("Uncovered normalized PM2.5 concentration: " + concentration);
    }

    private AirQualityIndex index(int value) {
        AqiCategory category = value <= 50 ? AqiCategory.GOOD
                : value <= 100 ? AqiCategory.MODERATE
                : value <= 150 ? AqiCategory.UNHEALTHY_FOR_SENSITIVE
                : value <= 200 ? AqiCategory.UNHEALTHY
                : value <= 300 ? AqiCategory.VERY_UNHEALTHY : AqiCategory.HAZARDOUS;
        return new AirQualityIndex(value, category);
    }
}
