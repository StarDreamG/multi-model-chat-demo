package com.afatguy.multimodelchat.model;

import com.afatguy.multimodelchat.model.ModelDtos.ModelView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/models")
public class ModelController {

    private final ModelService modelService;

    public ModelController(ModelService modelService) {
        this.modelService = modelService;
    }

    @GetMapping
    public List<ModelView> listEnabledModels() {
        return modelService.listEnabledModels();
    }
}