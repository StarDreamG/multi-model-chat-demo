package com.afatguy.multimodelchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MultiModelChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiModelChatApplication.class, args);
    }
}