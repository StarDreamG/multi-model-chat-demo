package com.afatguy.multimodelchat.persistence.repo;

import com.afatguy.multimodelchat.persistence.entity.ModelCallLogEntity;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelCallLogRepository extends JpaRepository<ModelCallLogEntity, Long> {

    List<ModelCallLogEntity> findByCreatedAtAfter(OffsetDateTime from);
}