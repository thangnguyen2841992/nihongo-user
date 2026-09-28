package com.thang.nihongo_user.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.List;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

class GeminiErrorTest {
    private UserServiceImpl service(HttpStatus status, String body) {
        WebClient client = WebClient.builder().baseUrl("https://example.test")
                .exchangeFunction(request -> Mono.just(ClientResponse.create(status)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE).body(body).build()))
                .build();
        var service = new UserServiceImpl(null, null, null, null, null, null, client, new ObjectMapper());
        ReflectionTestUtils.setField(service, "geminiKey", "diagnostic-test-key");
        ReflectionTestUtils.setField(service, "model", "test-model");
        return service;
    }

    @Test
    void logsGoogleErrorWithoutExposingConfiguredKey() {
        Logger logger = (Logger) LoggerFactory.getLogger(UserServiceImpl.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            var service = service(HttpStatus.FORBIDDEN,
                    "{\"error\":{\"status\":\"PERMISSION_DENIED\",\"message\":\"Key diagnostic-test-key is blocked\"}}");
            var error = assertThrows(ResponseStatusException.class, () -> service.analyzeJapanese("食べる").block());
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
            assertFalse(error.getReason().contains("diagnostic-test-key"));
            String logged = appender.list.get(0).getFormattedMessage();
            assertTrue(logged.contains("HTTP 403"));
            assertTrue(logged.contains("PERMISSION_DENIED"));
            assertTrue(logged.contains("[REDACTED] is blocked"));
            assertFalse(logged.contains("diagnostic-test-key"));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void preservesQuotaMessage() {
        var service = service(HttpStatus.TOO_MANY_REQUESTS,
                "{\"error\":{\"status\":\"RESOURCE_EXHAUSTED\",\"message\":\"Quota exceeded\"}}");
        var error = assertThrows(ResponseStatusException.class, () -> service.analyzeJapanese("食べる").block());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getStatusCode());
        assertEquals("AI đang đạt giới hạn sử dụng. Vui lòng thử lại sau.", error.getReason());
    }

    @Test
    void preservesHttpErrorWhenResponseIsNotJson() {
        var service = service(HttpStatus.FORBIDDEN, "<html>Access denied</html>");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                assertThrows(ResponseStatusException.class, () -> service.analyzeJapanese("食べる").block()).getStatusCode());
    }

    @Test
    void rejectsOversizedInput() {
        var service = service(HttpStatus.OK, "{}");
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, () -> service.analyzeJapanese("あ".repeat(2001)).block()).getStatusCode());
    }

    @Test
    void joinsTextPartsAndSkipsThoughts() throws Exception {
        String answer = new ObjectMapper().writeValueAsString(Map.of(
                "originalText", "学校", "translation", "Trường học", "reading", "がっこう",
                "sentenceStructure", "Một từ", "vocabulary", List.of(), "grammar", List.of(), "examples", List.of()));
        String body = new ObjectMapper().writeValueAsString(Map.of("candidates", List.of(Map.of(
                "finishReason", "STOP", "content", Map.of("parts", List.of(
                        Map.of("thought", true, "text", "Not JSON"),
                        Map.of("text", answer.substring(0, 20)), Map.of("text", answer.substring(20))))))));
        var result = service(HttpStatus.OK, body).analyzeJapanese("学校").block();
        assertEquals("学校", result.getOriginalText());
        assertEquals("Trường học", result.getTranslation());
    }

    @Test
    void handlesBlockedAndTruncatedOutput() {
        for (String reason : List.of("SAFETY", "MAX_TOKENS")) {
            var service = service(HttpStatus.OK, "{\"candidates\":[{\"finishReason\":\"" + reason + "\"}]}");
            var error = assertThrows(ResponseStatusException.class, () -> service.analyzeJapanese("学校").block());
            assertEquals(reason.equals("SAFETY") ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_GATEWAY,
                    error.getStatusCode());
        }
    }

    @Test
    void rejectsMalformedOrIncompleteSchema() throws Exception {
        for (String output : List.of("not JSON", "{\"originalText\":\"学校\"}")) {
            String body = new ObjectMapper().writeValueAsString(Map.of("candidates", List.of(Map.of(
                    "finishReason", "STOP", "content", Map.of("parts", List.of(Map.of("text", output)))))));
            assertEquals(HttpStatus.BAD_GATEWAY,
                    assertThrows(ResponseStatusException.class,
                            () -> service(HttpStatus.OK, body).analyzeJapanese("学校").block()).getStatusCode());
        }
    }
}
