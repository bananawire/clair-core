package com.claircore.analytics.infrastructure.config;

import com.claircore.analytics.domain.services.AqiCalculator;
import com.claircore.analytics.domain.services.MetricsAggregator;
import com.claircore.analytics.domain.services.TrendAnalyzer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the analytics domain services. They are plain objects with no framework annotations of
 * their own — the container has to be told about them here rather than finding them by scanning,
 * which is the point: the domain does not know it is running inside Spring.
 */
@Configuration
public class AnalyticsDomainServicesConfiguration {

    @Bean
    public AqiCalculator aqiCalculator() {
        return new AqiCalculator();
    }

    @Bean
    public TrendAnalyzer trendAnalyzer() {
        return new TrendAnalyzer();
    }

    @Bean
    public MetricsAggregator metricsAggregator(AqiCalculator aqiCalculator) {
        return new MetricsAggregator(aqiCalculator);
    }
}
