package com.afatguy.multimodelchat.model.provider;

import com.afatguy.multimodelchat.chat.ChatDtos.ChatCompletionRequest;
import com.afatguy.multimodelchat.persistence.entity.ModelConfigEntity;
import java.util.function.Consumer;

public interface ModelClient {

    boolean supports(String providerType);

    String chat(ModelConfigEntity modelConfig, ChatCompletionRequest request);

    default void streamChat(ModelConfigEntity modelConfig, ChatCompletionRequest request, Consumer<String> onDelta) {
        String full = chat(modelConfig, request);
        if (full != null && !full.isEmpty()) {
            onDelta.accept(full);
        }
    }

    default boolean healthCheck(ModelConfigEntity modelConfig) {
        return modelConfig.getEndpoint() != null && !modelConfig.getEndpoint().isBlank();
    }
}