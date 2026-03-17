package com.afatguy.multimodelchat.model.provider;

import com.afatguy.multimodelchat.chat.ChatDtos.ChatCompletionRequest;
import com.afatguy.multimodelchat.persistence.entity.ModelConfigEntity;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class VllmModelClient implements ModelClient {

    private final OpenAiCompatibleModelClient delegate;

    public VllmModelClient(OpenAiCompatibleModelClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean supports(String providerType) {
        return "VLLM".equalsIgnoreCase(providerType);
    }

    @Override
    public String chat(ModelConfigEntity modelConfig, ChatCompletionRequest request) {
        return delegate.chat(modelConfig, request);
    }

    @Override
    public void streamChat(ModelConfigEntity modelConfig, ChatCompletionRequest request, Consumer<String> onDelta) {
        delegate.streamChat(modelConfig, request, onDelta);
    }
}