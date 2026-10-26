package com.thang.nihongo_user.service;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits Japanese text into segments and gives every word written with kanji its hiragana reading (kuromoji/IPADIC)
 * and its Hán Việt reading (content/han-viet.tsv per kanji; whole words from the Mimi Kara N3 vocabulary first).
 * Okurigana stay outside the annotated part: 食べる → [食|た][べる].
 */
@Service
@Slf4j
public class JapaneseReadingService {
    /** One piece of the text; {@code reading}/{@code hanViet} are null for kana, Latin text and punctuation. */
    public record Segment(String text, String reading, String hanViet) {}

    private final Map<Integer, String> kanjiHanViet;
    private final Map<String, String> wordHanViet;
    private final Cache<String, List<Segment>> results = Caffeine.newBuilder()
            .maximumSize(5_000).expireAfterAccess(Duration.ofHours(6)).build();
    private volatile Tokenizer tokenizer;

    public JapaneseReadingService(ObjectMapper mapper) throws IOException {
        this.kanjiHanViet = loadKanji();
        this.wordHanViet = loadWords(mapper);
    }

    /** Loads the dictionary in the background so the first learner request is not the slow one. */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        var thread = new Thread(() -> {
            try {
                annotate("日本語を勉強します。");
            } catch (RuntimeException e) {
                log.warn("Kuromoji warm-up failed: {}", e.toString());
            }
        }, "kuromoji-warmup");
        thread.setDaemon(true);
        thread.start();
    }

    public List<Segment> annotate(String text) {
        if (text == null || text.isEmpty()) return List.of();
        return results.get(text, this::compute);
    }

    private List<Segment> compute(String text) {
        var segments = new ArrayList<Segment>();
        var plain = new StringBuilder();
        for (Token token : tokenizer().tokenize(text)) {
            String surface = token.getSurface();
            if (!hasKanji(surface)) {
                plain.append(surface);
                continue;
            }
            String reading = toHiragana(token.getReading());
            String normalized = toHiragana(surface);
            // Kana written around the kanji (okurigana, prefixes) are not part of the annotated core.
            int start = 0, end = surface.length(), readingStart = 0, readingEnd = reading == null ? 0 : reading.length();
            if (reading != null) {
                while (start < end && readingStart < readingEnd && !isKanji(surface.codePointAt(start))
                        && normalized.charAt(start) == reading.charAt(readingStart)) { start++; readingStart++; }
                while (end > start && readingEnd > readingStart && !isKanji(surface.codePointAt(end - 1))
                        && normalized.charAt(end - 1) == reading.charAt(readingEnd - 1)) { end--; readingEnd--; }
            }
            plain.append(surface, 0, start);
            String core = surface.substring(start, end);
            String coreReading = reading == null || readingEnd <= readingStart ? null : reading.substring(readingStart, readingEnd);
            if (coreReading != null && coreReading.equals(toHiragana(core))) coreReading = null;
            flush(segments, plain);
            segments.add(new Segment(core, coreReading, hanViet(surface, core)));
            plain.append(surface, end, surface.length());
        }
        flush(segments, plain);
        return List.copyOf(segments);
    }

    private String hanViet(String surface, String core) {
        String word = wordHanViet.get(surface);
        if (word == null) word = wordHanViet.get(core);
        if (word != null) return word;
        var parts = new ArrayList<String>();
        String previous = null;
        boolean known = false;
        for (int i = 0; i < core.length(); ) {
            int codePoint = core.codePointAt(i);
            i += Character.charCount(codePoint);
            if (codePoint == '々') {
                if (previous != null) parts.add(previous);
                continue;
            }
            if (!isKanji(codePoint)) continue;
            String reading = kanjiHanViet.get(codePoint);
            known |= reading != null;
            previous = reading == null ? "?" : reading;
            parts.add(previous);
        }
        return known ? String.join(" ", parts) : null;
    }

    private Tokenizer tokenizer() {
        Tokenizer current = tokenizer;
        if (current == null) {
            synchronized (this) {
                if (tokenizer == null) {
                    long started = System.nanoTime();
                    tokenizer = new Tokenizer();
                    log.info("Kuromoji dictionary loaded in {} ms", (System.nanoTime() - started) / 1_000_000);
                }
                current = tokenizer;
            }
        }
        return current;
    }

    private static void flush(List<Segment> segments, StringBuilder plain) {
        if (plain.isEmpty()) return;
        segments.add(new Segment(plain.toString(), null, null));
        plain.setLength(0);
    }

    static boolean isKanji(int codePoint) {
        return codePoint == '々' || Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }

    static boolean hasKanji(String text) {
        return text.codePoints().anyMatch(JapaneseReadingService::isKanji);
    }

    /** Katakana to hiragana; null for IPADIC's "unknown" marker. */
    static String toHiragana(String text) {
        if (text == null || text.isEmpty() || "*".equals(text)) return null;
        var result = new StringBuilder(text.length());
        for (char c : text.toCharArray()) result.append(c >= 'ァ' && c <= 'ヶ' ? (char) (c - 0x60) : c);
        return result.toString();
    }

    private static Map<Integer, String> loadKanji() throws IOException {
        var map = new HashMap<Integer, String>();
        try (var reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("content/han-viet.tsv").getInputStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null; ) {
                if (line.isBlank() || line.startsWith("#")) continue;
                int tab = line.indexOf('\t');
                if (tab <= 0) continue;
                map.put(line.codePointAt(0), line.substring(tab + 1).trim());
            }
        }
        return Map.copyOf(map);
    }

    private static Map<String, String> loadWords(ObjectMapper mapper) throws IOException {
        var map = new HashMap<String, String>();
        try (var input = new ClassPathResource("content/mimi-kara-n3-vocabulary.json").getInputStream()) {
            for (JsonNode card : mapper.readTree(input).path("cards")) {
                String word = card.path("word").asText(""), reading = card.path("sinoVietnamese").asText("");
                if (!word.isBlank() && !reading.isBlank() && hasKanji(word)) map.putIfAbsent(word.trim(), reading.trim().toUpperCase());
            }
        }
        return Map.copyOf(map);
    }
}
