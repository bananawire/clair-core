package com.claircore.nino.application.internal.commandservices;

import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelClient;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelReply;
import com.claircore.assistant.domain.exceptions.AssistantUnavailableException;
import com.claircore.nino.domain.model.valueobjects.HomeReadings;
import com.claircore.nino.domain.model.valueobjects.NinoAlert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NinoAlertServiceTest {

    @Mock
    private LanguageModelClient languageModelClient;

    @Test
    void returnsTheModelAnswerWhenTheModelReplies() {
        when(languageModelClient.complete(anyList()))
                .thenReturn(new LanguageModelReply("Vacía los baldes con agua hoy.", "deepseek-flash", 10, 12, 300));

        NinoAlert alert = new NinoAlertService(languageModelClient).generate(HomeReadings.empty());

        assertEquals("Vacía los baldes con agua hoy.", alert.message());
        assertEquals(NinoAlert.SOURCE_MODEL, alert.source());
    }

    @Test
    void returnsThePreWrittenAlertWhenTheModelIsUnavailable() {
        when(languageModelClient.complete(anyList()))
                .thenThrow(new AssistantUnavailableException("Assistant is not configured"));

        NinoAlert alert = new NinoAlertService(languageModelClient).generate(null);

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
