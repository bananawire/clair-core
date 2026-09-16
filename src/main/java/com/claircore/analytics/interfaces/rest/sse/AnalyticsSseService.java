package com.claircore.analytics.interfaces.rest.sse;

import com.claircore.analytics.domain.model.events.TelemetryReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AnalyticsSseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnalyticsSseService.class);
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter registerClient(UUID deviceId) {
        // Set a timeout of 2 minutes for connections
        SseEmitter emitter = new SseEmitter(120_000L);

        List<SseEmitter> deviceEmitters = emitters.computeIfAbsent(deviceId, k -> new CopyOnWriteArrayList<>());
        deviceEmitters.add(emitter);

        emitter.onCompletion(() -> {
            LOGGER.debug("SSE connection completed for device {}", deviceId);
            deviceEmitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            LOGGER.debug("SSE connection timed out for device {}", deviceId);
            deviceEmitters.remove(emitter);
        });

        emitter.onError((e) -> {
            LOGGER.debug("SSE connection error for device {}: {}", deviceId, e.getMessage());
            deviceEmitters.remove(emitter);
        });

        // Send initialization message to open the stream
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Stream connection established for device ID " + deviceId));
        } catch (Exception e) {
            deviceEmitters.remove(emitter);
        }

        return emitter;
    }

    @EventListener
    public void handleTelemetryReceived(TelemetryReceivedEvent event) {
        List<SseEmitter> deviceEmitters = emitters.get(event.deviceId());
        if (deviceEmitters == null || deviceEmitters.isEmpty()) {
            return;
        }

        LOGGER.info("Streaming new telemetry event for device {}", event.deviceId());
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : deviceEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("telemetry")
                        .data(event));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            deviceEmitters.removeAll(deadEmitters);
        }
    }
}
