package com.afatguy.multimodelchat.openai;

import com.afatguy.multimodelchat.chat.ChatDtos;
import com.afatguy.multimodelchat.chat.ChatDtos.ChatMessageInput;
import com.afatguy.multimodelchat.model.ModelService;
import com.afatguy.multimodelchat.model.provider.ModelGatewayService;
import com.afatguy.multimodelchat.openai.OpenAiDtos.ChatCompletionRequest;
import com.afatguy.multimodelchat.persistence.entity.ModelConfigEntity;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OpenAiService {

    private final ModelService modelService;
    private final ModelGatewayService modelGatewayService;

    public OpenAiService(ModelService modelService, ModelGatewayService modelGatewayService) {
        this.modelService = modelService;
        this.modelGatewayService = modelGatewayService;
    }

    public List<OpenAiDtos.ModelObject> listModels(String ownedBy) {
        return modelService.listEnabledModels().stream()
            .map(model -> OpenAiDtos.ModelObject.of(model.modelCode(), ownedBy))
            .toList();
    }

    public String chat(ChatCompletionRequest request) {
        ModelConfigEntity model = resolveEnabledModel(request);
        ChatDtos.ChatCompletionRequest gatewayRequest = toGatewayRequest(request);
        return modelGatewayService.chat(model, gatewayRequest);
    }

    public void stream(ChatCompletionRequest request, Consumer<String> onDelta) {
        ModelConfigEntity model = resolveEnabledModel(request);
        ChatDtos.ChatCompletionRequest gatewayRequest = toGatewayRequest(request);
        modelGatewayService.streamChat(model, gatewayRequest, onDelta);
    }

    public static String newCompletionId() {
        return "chatcmpl-" + UUID.randomUUID().toString().replace("-", "");
    }

    private ModelConfigEntity resolveEnabledModel(ChatCompletionRequest request) {
        String modelCode = request == null ? null : request.model();
        if (modelCode == null || modelCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing model");
        }
        if (!modelService.isModelEnabled(modelCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODEL_UNAVAILABLE");
        }
        return modelService.requireEntityByCode(modelCode);
    }

    private ChatDtos.ChatCompletionRequest toGatewayRequest(ChatCompletionRequest request) {
        List<ChatMessageInput> inputs = (request.messages() == null ? List.<OpenAiDtos.ChatMessage>of() : request.messages()).stream()
            .map(msg -> new ChatMessageInput(
                msg.role() == null ? "user" : msg.role(),
                msg.contentAsText()
            ))
            .toList();

        if (inputs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "messages is empty");
        }

        // sessionId is not required for upstream providers; we keep it as a placeholder.
        return new ChatDtos.ChatCompletionRequest(
            0L,
            request.model(),
            inputs,
            request.temperature(),
            request.topP(),
            request.maxTokens()
        );
    }
}

