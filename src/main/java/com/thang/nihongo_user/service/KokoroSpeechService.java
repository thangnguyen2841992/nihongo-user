package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * English pronunciation audio from a local Kokoro-82M engine speaking the OpenAI-compatible
 * {@code POST /v1/audio/speech} (scripts/kokoro/kokoro_server.py without Docker, or Kokoro-FastAPI). The engine may
 * answer MP3 or WAV; the format is detected from the bytes. Generated audio is cached on disk by voice and text, so a sentence is synthesized
 * once and then served from the file. The engine is CPU bound: a few syntheses run at once, identical requests share
 * one synthesis, and each learner may cause only a limited number of new syntheses per minute (cached audio is
 * unlimited). When the engine is not running the API answers 503 and the browser falls back to its own voice.
 */
@Service
public class KokoroSpeechService {
    public static final int MAX_TEXT_LENGTH = 300;
    private static final Pattern CONTROL = Pattern.compile("[\\p{Cc}\\p{Cf}&&[^\\n\\t]]");
    private static final Pattern LETTER = Pattern.compile("[A-Za-z]");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final URI engine;
    private final Path cacheDirectory;
    private final Map<String, String> voices;
    private final Semaphore engineSlots;
    private final int synthesesPerMinute;
    private final ConcurrentHashMap<String, CompletableFuture<byte[]>> inFlight = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Window> budgets = new ConcurrentHashMap<>();

    /** Audio bytes and their media type (audio/mpeg or audio/wav). */
    public record Speech(byte[] audio, String contentType) {}

    private static final class Window {
        long minute;
        int used;
    }

    @Autowired
    public KokoroSpeechService(
            @Value("${kokoro.url:http://127.0.0.1:8880}") String url,
            @Value("${kokoro.cache-dir:${user.home}/.nihongo/kokoro-cache}") String cacheDirectory,
            @Value("${kokoro.voice.uk:bf_emma}") String britishVoice,
            @Value("${kokoro.voice.us:af_heart}") String americanVoice,
            @Value("${kokoro.max-concurrent:2}") int maxConcurrent,
            @Value("${kokoro.syntheses-per-minute:20}") int synthesesPerMinute) {
        this.engine = URI.create(url.endsWith("/") ? url : url + "/");
        this.cacheDirectory = Path.of(cacheDirectory);
        this.voices = Map.of("uk", britishVoice, "us", americanVoice);
        this.engineSlots = new Semaphore(Math.max(1, maxConcurrent));
        this.synthesesPerMinute = Math.max(1, synthesesPerMinute);
    }

    /** Text as it will be spoken: whitespace collapsed; 422 when empty, too long or not English-looking. */
    public static String text(String raw) {
        String text = CONTROL.matcher(raw == null ? "" : raw).replaceAll("").replaceAll("\\s+", " ").trim();
        if (text.isEmpty() || text.length() > MAX_TEXT_LENGTH || !LETTER.matcher(text).find()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Câu cần đọc trống, quá dài (tối đa " + MAX_TEXT_LENGTH + " ký tự) hoặc không phải tiếng Anh.");
        }
        return text;
    }

    public String voice(String accent) {
        String voice = voices.get(accent == null ? "uk" : accent.trim().toLowerCase());
        if (voice == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giọng đọc không hợp lệ.");
        return voice;
    }

    /** Strong validator: changes with the text or the configured voice. */
    public String etag(String accent, String raw) {
        return "\"" + key(voice(accent), text(raw)) + "\"";
    }

    public Speech speech(String learner, String accent, String raw) {
        byte[] audio = audio(learner, accent, raw);
        return new Speech(audio, isWav(audio) ? "audio/wav" : "audio/mpeg");
    }

    private byte[] audio(String learner, String accent, String raw) {
        String voice = voice(accent);
        String text = text(raw);
        String key = key(voice, text);
        Path file = cacheDirectory.resolve(key + ".audio");
        try {
            if (Files.isRegularFile(file)) return Files.readAllBytes(file);
        } catch (IOException e) {
            throw unavailable(e);
        }
        var mine = new CompletableFuture<byte[]>();
        var running = inFlight.putIfAbsent(key, mine);
        if (running != null) return await(running);
        try {
            spendBudget(learner);
            byte[] audio = synthesizeAndStore(voice, text, file);
            mine.complete(audio);
            return audio;
        } catch (RuntimeException e) {
            mine.completeExceptionally(e);
            throw e;
        } finally {
            inFlight.remove(key, mine);
        }
    }

    private void spendBudget(String learner) {
        long minute = System.currentTimeMillis() / 60_000;
        var window = budgets.computeIfAbsent(learner == null ? "" : learner, ignored -> new Window());
        synchronized (window) {
            if (window.minute != minute) {
                window.minute = minute;
                window.used = 0;
            }
            if (window.used >= synthesesPerMinute) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Bạn đã tạo nhiều câu đọc mới trong một phút. Vui lòng thử lại sau ít giây.");
            }
            window.used++;
        }
        if (budgets.size() > 10_000) budgets.entrySet().removeIf(entry -> entry.getValue().minute < minute);
    }

    private byte[] synthesizeAndStore(String voice, String text, Path file) {
        boolean acquired = false;
        try {
            if (Files.isRegularFile(file)) return Files.readAllBytes(file);
            acquired = engineSlots.tryAcquire(20, TimeUnit.SECONDS);
            if (!acquired) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Máy đọc Kokoro đang bận. Vui lòng thử lại.");
            byte[] audio = synthesize(voice, text);
            Files.createDirectories(cacheDirectory);
            Path temporary = Files.createTempFile(cacheDirectory, "kokoro-", ".tmp");
            try {
                Files.write(temporary, audio);
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
            return audio;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (IOException e) {
            throw unavailable(e);
        } finally {
            if (acquired) engineSlots.release();
        }
    }

    protected byte[] synthesize(String voice, String text) throws IOException, InterruptedException {
        String body = JSON.writeValueAsString(Map.of(
                "model", "kokoro", "input", text, "voice", voice, "response_format", "mp3", "speed", 1.0));
        var response = client.send(HttpRequest.newBuilder(engine.resolve("v1/audio/speech"))
                        .timeout(Duration.ofSeconds(45)).header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        byte[] audio = response.body();
        if (response.statusCode() != 200 || !(isMp3(audio) || isWav(audio))) {
            throw new IOException("Kokoro returned invalid audio (HTTP " + response.statusCode() + ")");
        }
        return audio;
    }

    static boolean isMp3(byte[] audio) {
        if (audio == null || audio.length < 128) return false;
        boolean id3 = audio[0] == 'I' && audio[1] == 'D' && audio[2] == '3';
        boolean frame = (audio[0] & 0xFF) == 0xFF && (audio[1] & 0xE0) == 0xE0;
        return id3 || frame;
    }

    static boolean isWav(byte[] audio) {
        return audio != null && audio.length >= 44 && audio[0] == 'R' && audio[1] == 'I' && audio[2] == 'F' && audio[3] == 'F'
                && audio[8] == 'W' && audio[9] == 'A' && audio[10] == 'V' && audio[11] == 'E';
    }

    private static byte[] await(CompletableFuture<byte[]> running) {
        try {
            return running.get(70, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ResponseStatusException status) throw status;
            throw unavailable(e.getCause());
        } catch (TimeoutException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Máy đọc Kokoro đang bận. Vui lòng thử lại.", e);
        }
    }

    private static ResponseStatusException unavailable(Throwable cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Máy đọc Kokoro chưa sẵn sàng.", cause);
    }

    private static String key(String voice, String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(("kokoro-v1\n" + voice + "\n" + text).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
