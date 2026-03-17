package com.afatguy.multimodelchat.metrics;

import java.util.List;

public final class MetricsDtos {

    private MetricsDtos() {
    }

    public record ModelMetric(String modelCode, long calls, double successRate, double avgLatencyMs) {
    }

    public record MetricsOverview(String window, long totalCalls, double successRate, double avgLatencyMs, List<ModelMetric> byModel) {
    }
}