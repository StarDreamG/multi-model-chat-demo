package com.afatguy.multimodelchat.model.provider;

import com.afatguy.multimodelchat.chat.ChatDtos.ChatCompletionRequest;
import com.afatguy.multimodelchat.persistence.entity.ModelConfigEntity;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModelGatewayService {

    private final List<ModelClient> modelClients;

    public ModelGatewayService(List<ModelClient> modelClients) {
        this.modelClients = modelClients;
    }

    public String chat(ModelConfigEntity modelConfig, ChatCompletionRequest request) {
        ModelClient client = resolveClient(modelConfig.getProviderType());
        try {
            return client.chat(modelConfig, request);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "model provider call failed: " + ex.getMessage());
        }
    }

    public void streamChat(ModelConfigEntity modelConfig, ChatCompletionRequest request, Consumer<String> onDelta) {
        ModelClient client = resolveClient(modelConfig.getProviderType());
        try {
            client.streamChat(modelConfig, request, onDelta);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "model provider stream failed: " + ex.getMessage());
        }
    }

    public boolean healthCheck(ModelConfigEntity modelConfig) {
        try {
            return resolveClient(modelConfig.getProviderType()).healthCheck(modelConfig);
        } catch (Exception ex) {
            return false;
        }
    }

    private ModelClient resolveClient(String providerType) {
        return modelClients.stream()
            .filter(client -> client.supports(providerType))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported providerType: " + providerType));
    }
}