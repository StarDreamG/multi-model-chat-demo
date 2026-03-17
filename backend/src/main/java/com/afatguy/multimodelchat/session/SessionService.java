package com.afatguy.multimodelchat.session;

import com.afatguy.multimodelchat.persistence.entity.ChatMessageEntity;
import com.afatguy.multimodelchat.persistence.entity.ChatSessionEntity;
import com.afatguy.multimodelchat.persistence.repo.ChatMessageRepository;
import com.afatguy.multimodelchat.persistence.repo.ChatSessionRepository;
import com.afatguy.multimodelchat.session.SessionDtos.ChatMessageView;
import com.afatguy.multimodelchat.session.SessionDtos.CreateSessionRequest;
import com.afatguy.multimodelchat.session.SessionDtos.SessionPage;
import com.afatguy.multimodelchat.session.SessionDtos.SessionView;
import com.afatguy.multimodelchat.session.SessionDtos.UpdateSessionRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SessionService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;

    public SessionService(ChatSessionRepository sessionRepository, ChatMessageRepository messageRepository) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    public SessionPage listByUser(Long userId, int page, int pageSize) {
        List<SessionView> all = sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream().map(this::toSessionView).toList();

        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        int fromIndex = Math.max(0, (safePage - 1) * safePageSize);
        int toIndex = Math.min(all.size(), fromIndex + safePageSize);

        List<SessionView> items = fromIndex >= all.size() ? List.of() : all.subList(fromIndex, toIndex);
        return new SessionPage(items, safePage, safePageSize, all.size());
    }

    @Transactional
    public SessionView create(Long userId, CreateSessionRequest request) {
        ChatSessionEntity entity = new ChatSessionEntity();
        entity.setUserId(userId);
        entity.setTitle(request.title());
        entity.setModelCode(request.modelCode());
        return toSessionView(sessionRepository.save(entity));
    }

    @Transactional
    public SessionView update(Long userId, Long sessionId, UpdateSessionRequest request) {
        ChatSessionEntity entity = requireOwned(userId, sessionId);
        if (request.title() != null && !request.title().isBlank()) {
            entity.setTitle(request.title().trim());
        }
        if (request.modelCode() != null && !request.modelCode().isBlank()) {
            entity.setModelCode(request.modelCode().trim());
        }
        entity.touch();
        return toSessionView(sessionRepository.save(entity));
    }

    @Transactional
    public void delete(Long userId, Long sessionId) {
        requireOwned(userId, sessionId);
        messageRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteById(sessionId);
    }

    public List<ChatMessageView> listMessages(Long userId, Long sessionId) {
        requireOwned(userId, sessionId);
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream().map(this::toMessageView).toList();
    }

    @Transactional
    public ChatMessageView appendMessage(Long userId, Long sessionId, String role, String content, String modelCode) {
        ChatSessionEntity session = requireOwned(userId, sessionId);

        ChatMessageEntity message = new ChatMessageEntity();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setModelCode(modelCode);
        ChatMessageEntity saved = messageRepository.save(message);

        session.touch();
        sessionRepository.save(session);

        return toMessageView(saved);
    }

    public SessionView requireSession(Long userId, Long sessionId) {
        return toSessionView(requireOwned(userId, sessionId));
    }

    private ChatSessionEntity requireOwned(Long userId, Long sessionId) {
        return sessionRepository.findByIdAndUserId(sessionId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "session not found"));
    }

    private SessionView toSessionView(ChatSessionEntity entity) {
        return new SessionView(entity.getId(), entity.getTitle(), entity.getModelCode(), entity.getUpdatedAt());
    }

    private ChatMessageView toMessageView(ChatMessageEntity entity) {
        return new ChatMessageView(entity.getId(), entity.getRole(), entity.getContent(), entity.getModelCode(), entity.getCreatedAt());
    }
}