package com.afatguy.multimodelchat.persistence.repo;

import com.afatguy.multimodelchat.persistence.entity.ModelConfigEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelConfigRepository extends JpaRepository<ModelConfigEntity, Long> {

    List<ModelConfigEntity> findByEnabledTrueOrderByDisplayNameAsc();

    Optional<ModelConfigEntity> findByModelCode(String modelCode);

    boolean existsByModelCodeAndIdNot(String modelCode, Long id);

    boolean existsByModelCode(String modelCode);
}