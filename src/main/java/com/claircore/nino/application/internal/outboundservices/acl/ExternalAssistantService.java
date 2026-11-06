package com.claircore.nino.application.internal.outboundservices.acl;

import com.claircore.assistant.interfaces.acl.AssistantContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;

/** Consumer-side ACL for the language model, owned by the Assistant bounded context. */
@Service("ninoExternalAssistantService")
public class ExternalAssistantService {

    private final AssistantContextFacade assistantContextFacade;

    public ExternalAssistantService(AssistantContextFacade assistantContextFacade) {
        this.assistantContextFacade = assistantContextFacade;
    }

    public Optional<String> complete(String systemPrompt, String userMessage) {
        return assistantContextFacade.complete(systemPrompt, userMessage);
    }
}
