package com.afatguy.multimodelchat.persistence.repo;

import com.afatguy.multimodelchat.persistence.entity.ChatSessionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, Long> {

    List<ChatSessionEntity> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<ChatSessionEntity> findByIdAndUserId(Long id, Long userId);
}