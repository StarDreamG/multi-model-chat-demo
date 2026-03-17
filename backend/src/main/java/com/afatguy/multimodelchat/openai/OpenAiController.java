package com.afatguy.multimodelchat.openai;

import com.afatguy.multimodelchat.openai.OpenAiDtos.ChatCompletionChoice;
import com.afatguy.multimodelchat.openai.OpenAiDtos.ChatCompletionRequest;
import com.afatguy.multimodelchat.openai.OpenAiDtos.ChatCompletionResponse;
import com.afatguy.multimodelchat.openai.OpenAiDtos.ModelsListResponse;
import com.afatguy.multimodelchat.openai.OpenAiDtos.OpenAiError;
import com.afatguy.multimodelchat.openai.OpenAiDtos.Usage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1")
public class OpenAiController {

    private final OpenAiService openAiService;
    private final OpenAiServerProperties properties;
    private final ObjectMapper objectMapper;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    public OpenAiController(OpenAiService openAiService, OpenAiServerProperties properties, ObjectMapper objectMapper) {
        this.openAiService = openAiService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/models")
    public ModelsListResponse listModels() {
        String ownedBy = properties.ownedBy() == null || properties.ownedBy().isBlank()
            ? "multi-model-chat-backend"
            : properties.ownedBy();
        return ModelsListResponse.of(openAiService.listModels(ownedBy));
    }

    @PostMapping(value = "/chat/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object chatCompletions(@RequestBody ChatCompletionRequest request) {
        boolean stream = request != null && Boolean.TRUE.equals(request.stream());
        if (stream) {
            return streamChatCompletionsInternal(request);
        }
        return nonStreamChatCompletions(request);
    }

    private SseEmitter streamChatCompletionsInternal(ChatCompletionRequest request) {
        String completionId = OpenAiService.newCompletionId();
        long created = Instant.now().getEpochSecond();
        String model = request == null ? "" : request.model();
        SseEmitter emitter = new SseEmitter(180_000L);

        streamExecutor.execute(() -> {
            try {
                openAiService.stream(request, delta -> {
                    if (delta == null || delta.isEmpty()) {
                        return;
                    }
                    ChatCompletionResponse chunk = new ChatCompletionResponse(
                        completionId,
                        "chat.completion.chunk",
                        created,
                        model,
                        List.of(ChatCompletionChoice.deltaChoice(delta)),
                        null
                    );
                    sendOpenAiChunk(emitter, chunk);
                });

                ChatCompletionResponse done = new ChatCompletionResponse(
                    completionId,
                    "chat.completion.chunk",
                    created,
                    model,
                    List.of(ChatCompletionChoice.doneChoice()),
                    null
                );
                sendOpenAiChunk(emitter, done);
                emitter.send(SseEmitter.event().data("[DONE]"));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

    private ChatCompletionResponse nonStreamChatCompletions(ChatCompletionRequest request) {
        String completionId = OpenAiService.newCompletionId();
        long created = Instant.now().getEpochSecond();
        String model = request == null ? "" : request.model();

        String answer = openAiService.chat(request);
        int promptTokens = estimatePromptTokens(request);
        int completionTokens = estimateTokens(answer);

        return new ChatCompletionResponse(
            completionId,
            "chat.completion",
            created,
            model,
            List.of(ChatCompletionChoice.messageChoice(answer)),
            Usage.of(promptTokens, completionTokens)
        );
    }

    private void sendOpenAiChunk(SseEmitter emitter, ChatCompletionResponse chunk) {
        try {
            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(chunk)));
        } catch (IOException ioException) {
            throw new RuntimeException(ioException);
        }
    }

    private int estimatePromptTokens(ChatCompletionRequest request) {
        if (request == null || request.messages() == null) {
            return 0;
        }
        return request.messages().stream().mapToInt(msg -> estimateTokens(msg.contentAsText())).sum();
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<OpenAiDtos.OpenAiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = ex.getStatusCode() instanceof HttpStatus hs ? hs : HttpStatus.BAD_REQUEST;
        OpenAiDtos.OpenAiErrorResponse payload;
        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            payload = OpenAiError.invalidApiKey(ex.getReason() == null ? "unauthorized" : ex.getReason());
        } else if (status.is4xxClientError()) {
            payload = OpenAiError.invalidRequest(ex.getReason() == null ? "invalid request" : ex.getReason());
        } else {
            payload = OpenAiError.upstreamError(ex.getReason() == null ? "server error" : ex.getReason());
        }
        return ResponseEntity.status(status).body(payload);
    }
}
