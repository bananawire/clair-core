package com.claircore.localedge.infrastructure.config;

import com.claircore.localedge.application.internal.commandservices.LocalEdgeTelemetryCommandService;
import com.claircore.localedge.domain.services.SyntheticTelemetryGeneratorPolicy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class LocalEdgeConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LocalEdgeConfiguration.class))
            .withUserConfiguration(Stubs.class);

    @Test
    void wiringProducesTheGeneratorAndTheCommandServiceWhenEnabled() {
        runner.withPropertyValues(
                        "claircore.local-edge.enabled=true",
                        "claircore.local-edge.seed=1234")
                .run(context -> {
                    assertThat(context).hasSingleBean(SyntheticTelemetryGeneratorPolicy.class);
                    assertThat(context).hasSingleBean(LocalEdgeTelemetryCommandService.class);
                });
    }

    @Test
    void disablingSkipsTheBeanDefinitions() {
        runner.withPropertyValues("claircore.local-edge.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(SyntheticTelemetryGeneratorPolicy.class);
            assertThat(context).doesNotHaveBean(LocalEdgeTelemetryCommandService.class);
        });
    }

    @Test
    void missingPropertyKeepsTheBeanOutOfTheContext() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(SyntheticTelemetryGeneratorPolicy.class);
        });
    }

    @Configuration
    static class Stubs {
        @Bean
        com.claircore.localedge.application.internal.outboundservices.acl.ExternalDeviceService externalDeviceService() {
            return org.mockito.Mockito.mock(
                    com.claircore.localedge.application.internal.outboundservices.acl.ExternalDeviceService.class);
        }

        @Bean
        com.claircore.localedge.application.internal.outboundservices.acl.ExternalEvaluationService externalEvaluationService() {
            return org.mockito.Mockito.mock(
                    com.claircore.localedge.application.internal.outboundservices.acl.ExternalEvaluationService.class);
        }
    }
}
