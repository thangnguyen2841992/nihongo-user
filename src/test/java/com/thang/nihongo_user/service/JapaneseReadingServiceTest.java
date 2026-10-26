package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JapaneseReadingServiceTest {
    private final JapaneseReadingService service;

    JapaneseReadingServiceTest() throws Exception {
        service = new JapaneseReadingService(new ObjectMapper());
    }

    @Test void readsKanjiWordsAndKeepsOkuriganaOutside() {
        var segments = service.annotate("日本語を勉強します。毎日食べる。");
        assertEquals("日本語を勉強します。毎日食べる。",
                segments.stream().map(JapaneseReadingService.Segment::text).collect(Collectors.joining()));
        var eat = segments.stream().filter(s -> s.text().equals("食")).findFirst().orElseThrow();
        assertEquals("た", eat.reading());
        assertEquals("THỰC", eat.hanViet());
        var study = segments.stream().filter(s -> s.text().equals("勉強")).findFirst().orElseThrow();
        assertEquals("べんきょう", study.reading());
        assertEquals("MIỄN CƯỜNG", study.hanViet());
        assertTrue(segments.stream().filter(s -> s.text().equals("を")).allMatch(s -> s.reading() == null)
                || segments.stream().noneMatch(s -> s.text().equals("を")));
    }

    @Test void prefersTheVocabularyBookForWholeWords() {
        var man = service.annotate("男性").get(0);
        assertEquals("だんせい", man.reading());
        assertEquals("NAM TÍNH", man.hanViet());
    }

    @Test void textWithoutKanjiIsOnePlainSegment() {
        var segments = service.annotate("ありがとう、ABC");
        assertEquals(1, segments.size());
        assertNull(segments.get(0).reading());
    }
}
