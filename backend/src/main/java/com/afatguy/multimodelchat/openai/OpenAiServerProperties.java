package com.afatguy.multimodelchat.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.openai-server")
public record OpenAiServerProperties(
    String apiKey,
    String ownedBy
) {
}

