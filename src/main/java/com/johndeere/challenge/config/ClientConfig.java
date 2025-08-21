package com.johndeere.challenge.config;

import org.openapitools.client.api.PetApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

    @Bean
    PetApi petApi() {
        return new PetApi();
    }
}