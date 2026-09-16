package com.claircore.localedge.application.internal.outboundservices.acl;

import com.claircore.evaluation.interfaces.acl.EvaluationContextFacade;
import com.claircore.evaluation.interfaces.acl.TelemetryRecordingResult;
import com.claircore.evaluation.interfaces.acl.TelemetrySubmission;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Consumer-side ACL facade wrapping the Evaluation bounded context. The LocalEdge only ever records
 * {@code TelemetrySubmission}s and never reads back; failed submissions are reported via the
 * {@link TelemetryRecordingResult} and the cycle outcome reflects them.
 */
@Service("localedgeExternalEvaluationService")
@ConditionalOnProperty(name = "claircore.local-edge.enabled", havingValue = "true", matchIfMissing = false)
public class ExternalEvaluationService {

    private final EvaluationContextFacade evaluationContextFacade;

    public ExternalEvaluationService(EvaluationContextFacade evaluationContextFacade) {
        this.evaluationContextFacade = evaluationContextFacade;
    }

    public TelemetryRecordingResult recordTelemetry(TelemetrySubmission submission) {
        return evaluationContextFacade.recordTelemetry(submission);
    }
}
