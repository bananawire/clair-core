package com.claircore.localedge.domain.model.valueobjects;

/**
 * Which environmental threshold band the generator is targeting on this cycle.
 *
 * <p>The thresholds follow the localedge brief:
 * <ul>
 *   <li>{@link #RECOMMENDED} — PM2.5 [0,25], CO₂ [400,800], Temp [22,26], Humidity [40,60]</li>
 *   <li>{@link #ALERT}       — PM2.5 (25,50], CO₂ (800,1000], Temp [18,22)∪(26,28], Humidity [35,40)∪(60,65]</li>
 *   <li>{@link #HIGH}        — PM2.5 (50,1000], CO₂ (1000,5000], Temp [-10,18)∪(28,60], Humidity [0,35)∪(65,100]</li>
 * </ul>
 */
public enum EnvironmentalSeverity {
    RECOMMENDED,
    ALERT,
    HIGH
}
