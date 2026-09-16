package com.claircore.localedge.domain.model.valueobjects;

/**
 * How the LocalEdge mixes the three severity bands across cycles iterations.
 *
 * <p>{@link #MIXED} is the production default: ~60% RECOMMENDED, ~25% ALERT, ~15% HIGH, matching the
 * indoor air quality profile the bounded context was tuned against. {@link #RECOMMENDED_ONLY}
 * and {@link #STRESS_TEST} are diagnostic profiles used by tests and load probes.
 */
public enum SimulationScenario {
    MIXED(0.60, 0.25, 0.15),
    RECOMMENDED_ONLY(1.0, 0.0, 0.0),
    ALERT_ONLY(0.0, 1.0, 0.0),
    HIGH_ONLY(0.0, 0.0, 1.0),
    STRESS_TEST(0.10, 0.30, 0.60);

    private final double recommendedShare;
    private final double alertShare;
    private final double highShare;

    SimulationScenario(double recommendedShare, double alertShare, double highShare) {
        this.recommendedShare = recommendedShare;
        this.alertShare = alertShare;
        this.highShare = highShare;
    }

    public double recommendedShare() {
        return recommendedShare;
    }

    public double alertShare() {
        return alertShare;
    }

    public double highShare() {
        return highShare;
    }
}
