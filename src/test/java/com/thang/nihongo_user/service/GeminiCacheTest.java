package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class GeminiCacheTest {
    private JapaneseAiService service(ExchangeFunction exchange) {
        var service = new JapaneseAiService(WebClient.builder().baseUrl("https://example.test")
                .exchangeFunction(exchange).build(), new ObjectMapper());
        ReflectionTestUtils.setField(service, "geminiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");
        return service;
    }

    private ClientResponse answer() throws Exception {
        var mapper = new ObjectMapper();
        String output = mapper.writeValueAsString(Map.of("originalText", "学校", "reading", "がっこう",
                "translation", "Trường học", "sentenceStructure", "Một từ", "vocabulary", List.of(),
                "grammar", List.of(), "examples", List.of()));
        String response = mapper.writeValueAsString(Map.of("candidates", List.of(Map.of("finishReason", "STOP",
                "content", Map.of("parts", List.of(Map.of("text", output)))))));
        return ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json").body(response).build();
    }

    @Test
    void concurrentAndRepeatedSearchesCallGoogleOnce() throws Exception {
        var calls = new AtomicInteger();
        var response = Sinks.<ClientResponse>one();
        var service = service(request -> Mono.defer(() -> { calls.incrementAndGet(); return response.asMono(); }));
        var first = service.analyzeJapanese(" 学校 ").toFuture();
        var second = service.analyzeJapanese("学校").toFuture();
        assertEquals(1, calls.get());
        response.tryEmitValue(answer());
        assertEquals("Trường học", first.join().getTranslation());
        assertEquals("Trường học", second.join().getTranslation());
        assertEquals("がっこう", service.analyzeJapanese("学校").block().getReading());
        assertEquals(1, calls.get());
    }

    @Test
    void failureDoesNotPreventNextSearchFromRecovering() throws Exception {
        var calls = new AtomicInteger();
        ClientResponse success = answer();
        var service = service(request -> Mono.defer(() -> Mono.just(calls.incrementAndGet() == 1
                ? ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).body("{}").build() : success)));
        assertThrows(ResponseStatusException.class, () -> service.analyzeJapanese("学校").block());
        assertEquals("Trường học", service.analyzeJapanese("学校").block().getTranslation());
        assertEquals(2, calls.get());
    }

    @Test
    void cacheCanBeDisabledWithoutChangingResponse() throws Exception {
        var calls = new AtomicInteger();
        String body = answer().bodyToMono(String.class).block();
        var service = service(request -> {
            calls.incrementAndGet();
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json").body(body).build());
        });
        ReflectionTestUtils.setField(service, "cacheEnabled", false);
        assertNotNull(service.analyzeJapanese("学校").block());
        assertNotNull(service.analyzeJapanese("学校").block());
        assertEquals(2, calls.get());
    }
}
