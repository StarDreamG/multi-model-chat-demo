package com.afatguy.multimodelchat.model;

import com.afatguy.multimodelchat.model.ModelDtos.ModelAdminUpsertRequest;
import com.afatguy.multimodelchat.model.ModelDtos.ModelAdminView;
import com.afatguy.multimodelchat.model.ModelDtos.ModelView;
import com.afatguy.multimodelchat.persistence.entity.ModelConfigEntity;
import com.afatguy.multimodelchat.persistence.repo.ModelConfigRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModelService {

    private final ModelConfigRepository modelConfigRepository;

    public ModelService(ModelConfigRepository modelConfigRepository) {
        this.modelConfigRepository = modelConfigRepository;
    }

    public List<ModelView> listEnabledModels() {
        return modelConfigRepository.findByEnabledTrueOrderByDisplayNameAsc().stream().map(this::toModelView).toList();
    }

    public List<ModelAdminView> listAll() {
        return modelConfigRepository.findAll().stream().map(this::toAdminView).toList();
    }

    @Transactional
    public ModelAdminView create(ModelAdminUpsertRequest request) {
        if (modelConfigRepository.existsByModelCode(request.modelCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "modelCode already exists");
        }

        ModelConfigEntity entity = new ModelConfigEntity();
        applyUpsert(entity, request);
        return toAdminView(modelConfigRepository.save(entity));
    }

    @Transactional
    public ModelAdminView update(Long id, ModelAdminUpsertRequest request) {
        ModelConfigEntity existing = requireEntityById(id);
        if (modelConfigRepository.existsByModelCodeAndIdNot(request.modelCode(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "modelCode already exists");
        }
        applyUpsert(existing, request);
        return toAdminView(modelConfigRepository.save(existing));
    }

    @Transactional
    public ModelAdminView setEnabled(Long id, boolean enabled) {
        ModelConfigEntity existing = requireEntityById(id);
        existing.setEnabled(enabled);
        return toAdminView(modelConfigRepository.save(existing));
    }

    public boolean isModelEnabled(String modelCode) {
        return modelConfigRepository.findByModelCode(modelCode).map(ModelConfigEntity::getEnabled).orElse(false);
    }

    public ModelConfigEntity requireEntityByCode(String modelCode) {
        return modelConfigRepository.findByModelCode(modelCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "modelCode not found"));
    }

    public ModelConfigEntity requireEntityById(Long id) {
        return modelConfigRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "model not found"));
    }

    private void applyUpsert(ModelConfigEntity entity, ModelAdminUpsertRequest request) {
        entity.setModelCode(request.modelCode());
        entity.setDisplayName(request.displayName());
        entity.setProviderType(request.providerType());
        entity.setEndpoint(request.endpoint());
        // TODO: replace with real encryption/KMS.
        entity.setApiKeyEncrypted(request.apiKey());
        entity.setTimeoutMs(request.timeoutMs() == null ? 30000 : request.timeoutMs());
        entity.setMaxQps(request.maxQps() == null ? 20 : request.maxQps());
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setTags(request.tags() == null ? null : String.join(",", request.tags()));
    }

    private ModelView toModelView(ModelConfigEntity entity) {
        return new ModelView(entity.getModelCode(), entity.getDisplayName(), Boolean.TRUE.equals(entity.getEnabled()), splitTags(entity.getTags()));
    }

    private ModelAdminView toAdminView(ModelConfigEntity entity) {
        return new ModelAdminView(
            entity.getId(),
            entity.getModelCode(),
            entity.getDisplayName(),
            entity.getProviderType(),
            entity.getEndpoint(),
            entity.getTimeoutMs(),
            entity.getMaxQps(),
            Boolean.TRUE.equals(entity.getEnabled()),
            splitTags(entity.getTags()),
            entity.getUpdatedAt()
        );
    }

    private List<String> splitTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(",")).map(String::trim).filter(str -> !str.isEmpty()).toList();
    }
}