package com.afatguy.multimodelchat.metrics;

import com.afatguy.multimodelchat.chat.ChatService;
import com.afatguy.multimodelchat.metrics.MetricsDtos.MetricsOverview;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/metrics")
public class MetricsController {

    private final ChatService chatService;

    public MetricsController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/overview")
    public MetricsOverview overview() {
        return chatService.buildOverview();
    }
}