package com.afatguy.multimodelchat.session;

import com.afatguy.multimodelchat.security.SecurityUtils;
import com.afatguy.multimodelchat.session.SessionDtos.ChatMessageView;
import com.afatguy.multimodelchat.session.SessionDtos.CreateSessionRequest;
import com.afatguy.multimodelchat.session.SessionDtos.SessionPage;
import com.afatguy.multimodelchat.session.SessionDtos.SessionView;
import com.afatguy.multimodelchat.session.SessionDtos.UpdateSessionRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public SessionPage listSessions(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        return sessionService.listByUser(SecurityUtils.currentUserId(), page, pageSize);
    }

    @PostMapping
    public SessionView createSession(@Valid @RequestBody CreateSessionRequest request) {
        return sessionService.create(SecurityUtils.currentUserId(), request);
    }

    @PatchMapping("/{id}")
    public SessionView updateSession(@PathVariable("id") Long sessionId, @RequestBody UpdateSessionRequest request) {
        return sessionService.update(SecurityUtils.currentUserId(), sessionId, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable("id") Long sessionId) {
        sessionService.delete(SecurityUtils.currentUserId(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/messages")
    public List<ChatMessageView> listMessages(@PathVariable("id") Long sessionId) {
        return sessionService.listMessages(SecurityUtils.currentUserId(), sessionId);
    }
}