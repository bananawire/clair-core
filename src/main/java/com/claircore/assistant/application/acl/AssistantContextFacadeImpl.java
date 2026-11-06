package com.claircore.assistant.application.acl;

import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelClient;
import com.claircore.assistant.application.internal.outboundservices.llm.LanguageModelMessage;
import com.claircore.assistant.domain.exceptions.AssistantUnavailableException;
import com.claircore.assistant.interfaces.acl.AssistantContextFacade;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AssistantContextFacadeImpl implements AssistantContextFacade {

    private final LanguageModelClient languageModelClient;

    public AssistantContextFacadeImpl(LanguageModelClient languageModelClient) {
        this.languageModelClient = languageModelClient;
    }

    @Override
    public Optional<String> complete(String systemPrompt, String userMessage) {
        try {
            var reply = languageModelClient.complete(List.of(
                    LanguageModelMessage.system(systemPrompt),
                    LanguageModelMessage.user(userMessage)));
            return Optional.of(reply.content());
        } catch (AssistantUnavailableException e) {
            return Optional.empty();
        }
    }
}
