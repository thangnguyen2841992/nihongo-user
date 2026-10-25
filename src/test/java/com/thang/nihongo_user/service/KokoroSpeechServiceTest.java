package com.thang.nihongo_user.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class KokoroSpeechServiceTest {
    @TempDir Path cache;

    private static byte[] mp3() {
        byte[] audio = new byte[256];
        audio[0] = 'I'; audio[1] = 'D'; audio[2] = '3';
        return audio;
    }

    private static KokoroSpeechService service(HttpServer server, Path cache, int perMinute) {
        return new KokoroSpeechService("http://127.0.0.1:" + server.getAddress().getPort(), cache.toString(),
                "bf_emma", "af_heart", 2, perMinute);
    }

    @Test void synthesizesOnceThenServesTheCachedFile() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var calls = new AtomicInteger();
        var lastBody = new AtomicReference<String>();
        server.createContext("/v1/audio/speech", exchange -> {
            calls.incrementAndGet();
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] audio = mp3();
            exchange.sendResponseHeaders(200, audio.length);
            try (var body = exchange.getResponseBody()) { body.write(audio); }
        });
        server.start();
        try {
            var service = service(server, cache, 20);
            var first = service.speech("u1", "uk", "  My sister   is a nurse. ");
            assertEquals("audio/mpeg", first.contentType());
            assertArrayEquals(first.audio(), service.speech("u2", "uk", "My sister is a nurse.").audio());
            assertEquals(1, calls.get());
            assertTrue(lastBody.get().contains("\"voice\":\"bf_emma\""));
            assertTrue(lastBody.get().contains("\"input\":\"My sister is a nurse.\""));
            service.speech("u1", "us", "My sister is a nurse.");
            assertEquals(2, calls.get());
            assertTrue(lastBody.get().contains("\"voice\":\"af_heart\""));
            assertNotEquals(service.etag("uk", "My sister is a nurse."), service.etag("us", "My sister is a nurse."));
        } finally {
            server.stop(0);
        }
    }

    @Test void acceptsWavFromTheLocalServer() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/audio/speech", exchange -> {
            byte[] wav = new byte[64];
            System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, wav, 0, 4);
            System.arraycopy("WAVE".getBytes(StandardCharsets.US_ASCII), 0, wav, 8, 4);
            exchange.sendResponseHeaders(200, wav.length);
            try (var body = exchange.getResponseBody()) { body.write(wav); }
        });
        server.start();
        try {
            var service = service(server, cache, 20);
            assertEquals("audio/wav", service.speech("u1", "uk", "Hello").contentType());
            assertEquals("audio/wav", service.speech("u1", "uk", "Hello").contentType());
        } finally {
            server.stop(0);
        }
    }

    @Test void limitsNewSynthesesPerLearnerButNotCachedAudio() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/audio/speech", exchange -> {
            byte[] audio = mp3();
            exchange.sendResponseHeaders(200, audio.length);
            try (var body = exchange.getResponseBody()) { body.write(audio); }
        });
        server.start();
        try {
            var service = service(server, cache, 2);
            service.speech("u1", "uk", "one");
            service.speech("u1", "uk", "two");
            var limited = assertThrows(ResponseStatusException.class, () -> service.speech("u1", "uk", "three"));
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, limited.getStatusCode());
            assertDoesNotThrow(() -> service.speech("u1", "uk", "one"));
            assertDoesNotThrow(() -> service.speech("u2", "uk", "three"));
        } finally {
            server.stop(0);
        }
    }

    @Test void reportsUnavailableEngineAndRejectsBadInput() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/audio/speech", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        try {
            var service = service(server, cache, 20);
            var down = assertThrows(ResponseStatusException.class, () -> service.speech("u1", "uk", "Hello"));
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, down.getStatusCode());
        } finally {
            server.stop(0);
        }
        assertThrows(ResponseStatusException.class, () -> KokoroSpeechService.text("   "));
        assertThrows(ResponseStatusException.class, () -> KokoroSpeechService.text("123 456"));
        assertThrows(ResponseStatusException.class, () -> KokoroSpeechService.text("a".repeat(301)));
        var service = new KokoroSpeechService("http://127.0.0.1:1", cache.toString(), "bf_emma", "af_heart", 1, 1);
        assertThrows(ResponseStatusException.class, () -> service.voice("fr"));
        assertEquals("Hello world", KokoroSpeechService.text("Hello\u0000 \n world"));
    }
}
