package com.claircore.assistant.application.internal.outboundservices.acl;

import com.claircore.evaluation.interfaces.acl.EvaluationContextFacade;
import com.claircore.evaluation.interfaces.acl.TelemetryReading;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Consumer-side ACL for the latest reading of each device, owned by the Evaluation bounded context. */
@Service("assistantExternalEvaluationService")
public class ExternalEvaluationService {

    private final EvaluationContextFacade evaluationContextFacade;

    public ExternalEvaluationService(EvaluationContextFacade evaluationContextFacade) {
        this.evaluationContextFacade = evaluationContextFacade;
    }

    public Optional<TelemetryReading> findLatestReading(UUID deviceId, Instant visibleSince) {
        return evaluationContextFacade.getLatestReading(deviceId, visibleSince);
    }
}
