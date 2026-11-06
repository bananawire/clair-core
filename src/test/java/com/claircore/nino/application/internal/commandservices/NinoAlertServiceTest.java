package com.claircore.nino.application.internal.commandservices;

import com.claircore.nino.application.internal.outboundservices.acl.ExternalAssistantService;
import com.claircore.nino.domain.model.valueobjects.HomeReadings;
import com.claircore.nino.domain.model.valueobjects.NinoAlert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NinoAlertServiceTest {

    @Mock
    private ExternalAssistantService externalAssistantService;

    @Test
    void returnsTheModelAnswerWhenTheModelReplies() {
        when(externalAssistantService.complete(anyString(), anyString()))
                .thenReturn(Optional.of("Vacía los baldes con agua hoy."));

        NinoAlert alert = new NinoAlertService(externalAssistantService).generate(HomeReadings.empty());

        assertEquals("Vacía los baldes con agua hoy.", alert.message());
        assertEquals(NinoAlert.SOURCE_MODEL, alert.source());
    }

    @Test
    void returnsThePreWrittenAlertWhenTheModelIsUnavailable() {
        when(externalAssistantService.complete(anyString(), anyString())).thenReturn(Optional.empty());

        NinoAlert alert = new NinoAlertService(externalAssistantService).generate(null);

        assertEquals(NinoAlertService.FALLBACK_MESSAGE, alert.message());
        assertEquals(NinoAlert.SOURCE_FALLBACK, alert.source());
    }

    @Test
    void fillsMissingReadingsWithTheDemoScenario() {
        String prompt = NinoAlertService.describe(new HomeReadings(90.0, null, null, null, null, null));

        assertTrue(prompt.contains("Humedad: 90 %"));
        assertTrue(prompt.contains("30.0 °C"));
        assertTrue(prompt.contains("ALTO (82/100)"));
        assertTrue(!prompt.contains("Lluvia"));
    }

    @Test
    void mentionsHydrologyOnlyWhenTheStationSendsIt() {
        String prompt = NinoAlertService.describe(new HomeReadings(null, null, null, null, 35.5, 120.0));

        assertTrue(prompt.contains("Lluvia en el último intervalo: 35.5 mm."));
        assertTrue(prompt.contains("Nivel de agua cerca de la vivienda: 120 cm."));
    }
}
