package com.claircore.alerting.application.internal.outboundservices.acl;

import com.claircore.evaluation.interfaces.acl.EvaluationContextFacade;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Consumer-side ACL: alerting tells evaluation which readings it has finished with. */
@Service
public class ExternalAlertingEvaluationService {
    private final EvaluationContextFacade evaluationContextFacade;

    public ExternalAlertingEvaluationService(EvaluationContextFacade evaluationContextFacade) {
        this.evaluationContextFacade = evaluationContextFacade;
    }

    public void markAlertsEvaluated(UUID deviceId, UUID readingId) {
        evaluationContextFacade.markAlertsEvaluated(deviceId, readingId);
    }
}
