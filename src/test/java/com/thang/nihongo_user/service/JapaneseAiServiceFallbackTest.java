package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.junit.jupiter.api.Assertions.*;

class JapaneseAiServiceFallbackTest {
    private static final String OVERLOADED = """
            {"error":{"code":503,"status":"UNAVAILABLE","message":"This model is currently experiencing high demand."}}""";

    private final List<String> calledModels = new ArrayList<>();
    private final ConcurrentLinkedDeque<Integer> statuses = new ConcurrentLinkedDeque<>();

    private static String success() throws Exception {
        var analysis = Map.of("originalText", "猫", "translation", "con mèo", "reading", "ねこ",
                "sentenceStructure", "Danh từ", "vocabulary", List.of(), "grammar", List.of(), "examples", List.of());
        var text = new ObjectMapper().writeValueAsString(analysis);
        return new ObjectMapper().writeValueAsString(Map.of("candidates", List.of(Map.of(
                "finishReason", "STOP", "content", Map.of("parts", List.of(Map.of("text", text)))))));
    }

    /** Fake Gemini: answers with the next queued status, and records which model each request asked for. */
    private JapaneseAiService service(String fallback, Integer... replies) throws Exception {
        statuses.addAll(List.of(replies));
        String ok = success();
        var client = WebClient.builder().exchangeFunction(request -> {
            String path = request.url().getPath();
            calledModels.add(path.substring(path.lastIndexOf('/') + 1, path.indexOf(':')));
            int status = statuses.isEmpty() ? 200 : statuses.poll();
            return Mono.just(ClientResponse.create(HttpStatus.valueOf(status))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(status == 200 ? ok : OVERLOADED).build());
        }).build();
        var service = new JapaneseAiService(client, new ObjectMapper());
        ReflectionTestUtils.setField(service, "model", "gemini-3.1-flash-lite");
        ReflectionTestUtils.setField(service, "fallbackModel", fallback);
        ReflectionTestUtils.setField(service, "geminiKey", "test-key");
        ReflectionTestUtils.setField(service, "cacheEnabled", false);
        service.retryDelay = Duration.ofMillis(1);
        return service;
    }

    @Test void retriesTheSameModelWhenItIsBrieflyOverloaded() throws Exception {
        var result = service("gemini-3.5-flash-lite", 503, 503).analyzeJapanese("猫").block();
        assertEquals("con mèo", result.getTranslation());
        assertEquals(List.of("gemini-3.1-flash-lite", "gemini-3.1-flash-lite", "gemini-3.1-flash-lite"), calledModels);
    }

    @Test void switchesToTheFallbackModelWhenTheMainModelStaysOverloaded() throws Exception {
        var result = service("gemini-3.5-flash-lite", 503, 503, 503).analyzeJapanese("猫").block();
        assertEquals("ねこ", result.getReading());
        assertEquals(4, calledModels.size());
        assertEquals("gemini-3.5-flash-lite", calledModels.get(3));
    }

    @Test void usesTheFallbackRightAwayWhenTheMainModelIsOverQuota() throws Exception {
        service("gemini-3.5-flash-lite", 429).analyzeJapanese("猫").block();
        assertEquals(List.of("gemini-3.1-flash-lite", "gemini-3.5-flash-lite"), calledModels);
    }

    @Test void reportsOverloadClearlyWhenBothModelsAreBusy() throws Exception {
        var service = service("gemini-3.5-flash-lite", 503, 503, 503, 503, 503, 503);
        var error = assertThrows(ResponseStatusException.class, () -> service.analyzeJapanese("猫").block());
        assertEquals(503, error.getStatusCode().value());
        assertEquals("AI đang quá tải. Vui lòng thử lại sau ít phút.", error.getReason());
        assertEquals(6, calledModels.size());
    }

    @Test void withoutAFallbackOnlyTheMainModelIsTried() throws Exception {
        var service = service("", 503, 503, 503);
        assertThrows(ResponseStatusException.class, () -> service.analyzeJapanese("猫").block());
        assertEquals(3, calledModels.size());
    }
}
