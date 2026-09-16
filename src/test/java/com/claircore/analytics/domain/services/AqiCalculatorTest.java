package com.claircore.analytics.domain.services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AqiCalculatorTest {
    private final AqiCalculator calculator = new AqiCalculator();

    @Test void matchesEpaBreakpointsAfterTruncation() {
        double[] pm = {0, 9, 9.09, 9.1, 35.4, 35.5, 55.4, 55.5, 125.4, 125.5, 225.4, 225.5, 325.4};
        int[] aqi =   {0, 50, 50, 51, 100, 101, 150, 151, 200, 201, 300, 301, 500};
        for (int i = 0; i < pm.length; i++) assertEquals(aqi[i], calculator.calculateAqi(pm[i]).value(), "PM=" + pm[i]);
        assertEquals(44, calculator.calculateAqi(8.0).value());
        assertEquals(56, calculator.calculateAqi(12.05).value());
    }

    @Test void sweepHasNoGapSpikesOrDecreases() {
        int previous = -1;
        for (int hundredths = 0; hundredths <= 100_000; hundredths++) {
            double pm = hundredths / 100.0;
            int value = calculator.calculateAqi(pm).value();
            assertTrue(value >= previous, "Decrease at PM=" + pm);
            assertTrue(value <= 500);
            previous = value;
        }
        assertEquals(500, calculator.calculateAqi(Double.MAX_VALUE).value());
    }

    @Test void invalidAndMissingMeasurementsHaveNoIndex() {
        for (Double pm : new Double[]{null, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> calculator.calculateAqi(pm));
    }
}
