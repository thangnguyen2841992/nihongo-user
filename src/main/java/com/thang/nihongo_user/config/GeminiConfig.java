package com.thang.nihongo_user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class GeminiConfig {
    @Bean
    public WebClient geminiWebClient(
            @Value("${gemini.base-url}") String baseUrl,
            @Value("${gemini.api-key}") String apiKey
    ) {

        HttpClient httpClient =
                HttpClient.create()
                        .responseTimeout(
                                Duration.ofSeconds(60)
                        );

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(
                        new ReactorClientHttpConnector(
                                httpClient
                        )
                )
                .defaultHeader(
                        "x-goog-api-key",
                        apiKey
                )
                .defaultHeader(
                        "Content-Type",
                        "application/json"
                )
                .build();
    }
}
