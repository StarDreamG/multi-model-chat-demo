package com.afatguy.multimodelchat.chat;

import com.afatguy.multimodelchat.chat.ChatDtos.ChatCompletionRequest;
import com.afatguy.multimodelchat.chat.ChatDtos.ChatCompletionResponse;
import com.afatguy.multimodelchat.security.SecurityUtils;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/completions")
    public ChatCompletionResponse completion(@Valid @RequestBody ChatCompletionRequest request) {
        return chatService.complete(SecurityUtils.currentUserId(), request);
    }

    @PostMapping(value = "/completions/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter completionStream(@Valid @RequestBody ChatCompletionRequest request) {
        Long userId = SecurityUtils.currentUserId();
        String requestId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(180_000L);

        streamExecutor.execute(() -> {
            try {
                emitter.send(SseEmitter.event().name("start").data(requestId));
                chatService.stream(userId, request, requestId, delta -> {
                    try {
                        emitter.send(SseEmitter.event().name("delta").data(delta));
                    } catch (IOException ioException) {
                        throw new RuntimeException(ioException);
                    }
                });
                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }
}