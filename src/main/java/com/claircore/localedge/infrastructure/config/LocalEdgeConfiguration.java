package com.claircore.localedge.infrastructure.config;

import com.claircore.localedge.application.internal.commandservices.LocalEdgeTelemetryCommandService;
import com.claircore.localedge.application.internal.commandservices.LocalEdgeTelemetryCommandServiceImpl;
import com.claircore.localedge.domain.services.SyntheticTelemetryGeneratorPolicy;
import com.claircore.localedge.application.LocalDeviceCommandExecutor;
import com.claircore.localedge.infrastructure.simulation.SimulatedLocalDeviceCommandExecutor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the LocalEdge bounded context into the application context. The beans are gated by
 * {@code claircore.local-edge.enabled=true} so the BC is inert in production unless explicitly
 * turned on; this matches the brief's "default false in production" requirement.
 *
 * <p>The seed is the same property that the scheduler reads; the generator and the scheduler
 * agree because both resolve {@code claircore.local-edge.seed} with default 0.
 */
@Configuration
@ConditionalOnProperty(
        name = "claircore.local-edge.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class LocalEdgeConfiguration {

    @Bean
    public SyntheticTelemetryGeneratorPolicy syntheticTelemetryGeneratorPolicy(
            @org.springframework.beans.factory.annotation.Value("${claircore.local-edge.seed:0}") long seed) {
        return new SyntheticTelemetryGeneratorPolicy(seed);
    }

    /**
     * The simulator is the safe default for this in-process deployment. A physical adapter can
     * replace it simply by providing another {@link LocalDeviceCommandExecutor} bean.
     */
    @Bean
    @ConditionalOnMissingBean(LocalDeviceCommandExecutor.class)
    public LocalDeviceCommandExecutor localDeviceCommandExecutor() {
        return new SimulatedLocalDeviceCommandExecutor();
    }

    @Bean
    public LocalEdgeTelemetryCommandService localEdgeTelemetryCommandService(
            com.claircore.localedge.application.internal.outboundservices.acl.ExternalDeviceService externalDeviceService,
            com.claircore.localedge.application.internal.outboundservices.acl.ExternalEvaluationService externalEvaluationService,
            SyntheticTelemetryGeneratorPolicy generator, LocalDeviceCommandExecutor commandExecutor) {
        return new LocalEdgeTelemetryCommandServiceImpl(externalDeviceService, externalEvaluationService, generator, commandExecutor);
    }
}
