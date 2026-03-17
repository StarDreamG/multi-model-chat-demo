package com.afatguy.multimodelchat.model;

import com.afatguy.multimodelchat.model.ModelDtos.EnableRequest;
import com.afatguy.multimodelchat.model.ModelDtos.HealthCheckResult;
import com.afatguy.multimodelchat.model.ModelDtos.ModelAdminUpsertRequest;
import com.afatguy.multimodelchat.model.ModelDtos.ModelAdminView;
import com.afatguy.multimodelchat.model.provider.ModelGatewayService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/models")
public class AdminModelController {

    private final ModelService modelService;
    private final ModelGatewayService modelGatewayService;

    public AdminModelController(ModelService modelService, ModelGatewayService modelGatewayService) {
        this.modelService = modelService;
        this.modelGatewayService = modelGatewayService;
    }

    @GetMapping
    public List<ModelAdminView> listAll() {
        return modelService.listAll();
    }

    @PostMapping
    public ModelAdminView create(@Valid @RequestBody ModelAdminUpsertRequest request) {
        return modelService.create(request);
    }

    @PutMapping("/{id}")
    public ModelAdminView update(@PathVariable Long id, @Valid @RequestBody ModelAdminUpsertRequest request) {
        return modelService.update(id, request);
    }

    @PatchMapping("/{id}/enable")
    public ModelAdminView setEnabled(@PathVariable Long id, @Valid @RequestBody EnableRequest request) {
        return modelService.setEnabled(id, request.enabled());
    }

    @PostMapping("/{id}/health-check")
    public HealthCheckResult healthCheck(@PathVariable Long id) {
        boolean ok = modelGatewayService.healthCheck(modelService.requireEntityById(id));
        return new HealthCheckResult(ok, ok ? "reachable" : "unreachable", OffsetDateTime.now());
    }
}