package com.afatguy.multimodelchat.chat;

import com.afatguy.multimodelchat.chat.ChatDtos.ChatCompletionRequest;
import com.afatguy.multimodelchat.chat.ChatDtos.ChatCompletionResponse;
import com.afatguy.multimodelchat.chat.ChatDtos.TokenUsage;
import com.afatguy.multimodelchat.metrics.MetricsDtos.MetricsOverview;
import com.afatguy.multimodelchat.metrics.MetricsDtos.ModelMetric;
import com.afatguy.multimodelchat.model.ModelService;
import com.afatguy.multimodelchat.model.provider.ModelGatewayService;
import com.afatguy.multimodelchat.persistence.entity.ModelCallLogEntity;
import com.afatguy.multimodelchat.persistence.entity.ModelConfigEntity;
import com.afatguy.multimodelchat.persistence.repo.ModelCallLogRepository;
import com.afatguy.multimodelchat.session.SessionDtos.ChatMessageView;
import com.afatguy.multimodelchat.session.SessionService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChatService {

    private final SessionService sessionService;
    private final ModelService modelService;
    private final ModelGatewayService modelGatewayService;
    private final ModelCallLogRepository modelCallLogRepository;

    public ChatService(
        SessionService sessionService,
        ModelService modelService,
        ModelGatewayService modelGatewayService,
        ModelCallLogRepository modelCallLogRepository
    ) {
        this.sessionService = sessionService;
        this.modelService = modelService;
        this.modelGatewayService = modelGatewayService;
        this.modelCallLogRepository = modelCallLogRepository;
    }

    @Transactional
    public ChatCompletionResponse complete(Long userId, ChatCompletionRequest request) {
        String requestId = UUID.randomUUID().toString();
        return completeWithRequestId(userId, request, requestId);
    }

    @Transactional
    public ChatCompletionResponse completeWithRequestId(Long userId, ChatCompletionRequest request, String requestId) {
        long start = System.currentTimeMillis();
        validateRequest(userId, request);

        String prompt = lastUserMessage(request);
        sessionService.appendMessage(userId, request.sessionId(), "user", prompt, request.modelCode());

        ModelConfigEntity model = modelService.requireEntityByCode(request.modelCode());

        try {
            String answer = modelGatewayService.chat(model, request);
            ChatMessageView assistantMessage = sessionService.appendMessage(userId, request.sessionId(), "assistant", answer, request.modelCode());

            int promptTokens = estimateTokens(prompt);
            int completionTokens = estimateTokens(answer);
            persistLog(requestId, userId, request.sessionId(), request.modelCode(), promptTokens, completionTokens,
                (int) (System.currentTimeMillis() - start), "SUCCESS", null, null);

            return new ChatCompletionResponse(requestId, assistantMessage, new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens));
        } catch (Exception ex) {
            persistLog(requestId, userId, request.sessionId(), request.modelCode(), estimateTokens(prompt), 0,
                (int) (System.currentTimeMillis() - start), "ERROR", "PROVIDER_ERROR", ex.getMessage());
            if (ex instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "model provider call failed");
        }
    }

    public void stream(Long userId, ChatCompletionRequest request, String requestId, Consumer<String> onDelta) {
        long start = System.currentTimeMillis();
        validateRequest(userId, request);

        String prompt = lastUserMessage(request);
        sessionService.appendMessage(userId, request.sessionId(), "user", prompt, request.modelCode());

        ModelConfigEntity model = modelService.requireEntityByCode(request.modelCode());
        StringBuilder fullAnswer = new StringBuilder();

        try {
            modelGatewayService.streamChat(model, request, delta -> {
                if (delta == null || delta.isEmpty()) {
                    return;
                }
                fullAnswer.append(delta);
                onDelta.accept(delta);
            });

            String answer = fullAnswer.toString();
            sessionService.appendMessage(userId, request.sessionId(), "assistant", answer, request.modelCode());

            int promptTokens = estimateTokens(prompt);
            int completionTokens = estimateTokens(answer);
            persistLog(requestId, userId, request.sessionId(), request.modelCode(), promptTokens, completionTokens,
                (int) (System.currentTimeMillis() - start), "SUCCESS", null, null);
        } catch (Exception ex) {
            persistLog(requestId, userId, request.sessionId(), request.modelCode(), estimateTokens(prompt), estimateTokens(fullAnswer.toString()),
                (int) (System.currentTimeMillis() - start), "ERROR", "PROVIDER_STREAM_ERROR", ex.getMessage());
            if (ex instanceof ResponseStatusException rse) {
                throw rse;
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "model provider stream failed");
        }
    }

    public MetricsOverview buildOverview() {
        OffsetDateTime from = OffsetDateTime.now().minusHours(24);
        List<ModelCallLogEntity> logs = modelCallLogRepository.findByCreatedAtAfter(from);

        long total = logs.size();
        if (total == 0) {
            return new MetricsOverview("last_24h", 0L, 0.0, 0.0, List.of());
        }

        long successCount = logs.stream().filter(log -> "SUCCESS".equalsIgnoreCase(log.getStatus())).count();
        double successRate = successCount * 100.0 / total;
        double avgLatency = logs.stream().mapToInt(ModelCallLogEntity::getDurationMs).average().orElse(0.0);

        Map<String, List<ModelCallLogEntity>> grouped = logs.stream()
            .collect(java.util.stream.Collectors.groupingBy(ModelCallLogEntity::getModelCode));

        List<ModelMetric> byModel = grouped.entrySet().stream().map(entry -> {
            long count = entry.getValue().size();
            long ok = entry.getValue().stream().filter(log -> "SUCCESS".equalsIgnoreCase(log.getStatus())).count();
            double sr = count == 0 ? 0.0 : ok * 100.0 / count;
            double latency = entry.getValue().stream().mapToInt(ModelCallLogEntity::getDurationMs).average().orElse(0.0);
            return new ModelMetric(entry.getKey(), count, sr, latency);
        }).sorted(java.util.Comparator.comparing(ModelMetric::modelCode)).toList();

        return new MetricsOverview("last_24h", total, successRate, avgLatency, byModel);
    }

    private void validateRequest(Long userId, ChatCompletionRequest request) {
        if (request == null || request.sessionId() == null || request.modelCode() == null || request.modelCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid chat request");
        }
        sessionService.requireSession(userId, request.sessionId());
        if (!modelService.isModelEnabled(request.modelCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MODEL_UNAVAILABLE");
        }
    }

    private String lastUserMessage(ChatCompletionRequest request) {
        return request.messages().stream()
            .filter(msg -> "user".equalsIgnoreCase(msg.role()))
            .reduce((first, second) -> second)
            .map(ChatDtos.ChatMessageInput::content)
            .orElse("");
    }

    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private void persistLog(
        String requestId,
        Long userId,
        Long sessionId,
        String modelCode,
        Integer requestTokens,
        Integer responseTokens,
        Integer durationMs,
        String status,
        String errorCode,
        String errorMessage
    ) {
        ModelCallLogEntity log = new ModelCallLogEntity();
        log.setRequestId(requestId);
        log.setUserId(userId);
        log.setSessionId(sessionId);
        log.setModelCode(modelCode);
        log.setRequestTokens(requestTokens);
        log.setResponseTokens(responseTokens);
        log.setDurationMs(durationMs);
        log.setStatus(status);
        log.setErrorCode(errorCode);
        log.setErrorMessage(errorMessage);
        modelCallLogRepository.save(log);
    }
}