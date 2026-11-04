package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.VocabularyQuizAttempt;
import com.thang.nihongo_user.repository.VocabularyQuizAttemptRepository;
import com.thang.nihongo_user.service.VocabularyQuizService.Answer;
import com.thang.nihongo_user.service.VocabularyQuizService.Kind;
import com.thang.nihongo_user.service.VocabularyQuizService.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VocabularyQuizServiceTest {
    private final VocabularyDeckService decks = mock(VocabularyDeckService.class);
    private final VocabularyQuizAttemptRepository attempts = mock(VocabularyQuizAttemptRepository.class);
    private final VocabularyQuizService service = new VocabularyQuizService(decks, attempts);

    private static VocabularyDeckService.Card card(int number, int unit, String word, String reading, String meaning) {
        return new VocabularyDeckService.Card(number, unit, word, reading, meaning, "", "", "");
    }

    @BeforeEach void setUp() {
        service.clock = Clock.fixed(Instant.parse("2026-11-03T01:00:00Z"), ZoneOffset.UTC);
        when(decks.get("owner", 42L)).thenReturn(new VocabularyDeckService.Deck("minna", "Minna II", "N4", 3, List.of(
                card(1, 26, "運動会", "うんどうかい", "hội thao"),
                card(2, 26, "いつでも", "", "bất cứ lúc nào"),
                card(3, 27, "飼います", "かいます", "nuôi"))));
        when(attempts.bestPercent(any(), any(), any(), anyInt())).thenReturn(-1);
        when(attempts.save(any())).thenAnswer(invocation -> {
            VocabularyQuizAttempt attempt = invocation.getArgument(0);
            attempt.setId(9L);
            return attempt;
        });
    }

    @Test void gradesChosenAnswersAgainstTheDeckAndStoresTheScore() {
        var result = service.submit("owner", 42L, new Submission(26, List.of(
                new Answer(1, Kind.MEANING, "hội thao"),
                new Answer(1, Kind.READING, "うんどうば"),
                new Answer(2, Kind.MEANING, " bất cứ lúc nào "))));

        assertEquals(3, result.total());
        assertEquals(2, result.correct());
        assertEquals(66, result.percent());
        assertEquals(66, result.bestPercent());
        assertFalse(result.newBest());
        var wrong = result.answers().get(1);
        assertFalse(wrong.correct());
        assertEquals("うんどうかい", wrong.expected());
        assertEquals("運動会", wrong.word());

        var saved = ArgumentCaptor.forClass(VocabularyQuizAttempt.class);
        verify(attempts).save(saved.capture());
        assertEquals("owner", saved.getValue().getUserId());
        assertEquals("minna", saved.getValue().getDeckId());
        assertEquals(26, saved.getValue().getUnit());
        assertEquals(2, saved.getValue().getCorrect());
        assertEquals(LocalDateTime.of(2026, 11, 3, 1, 0), saved.getValue().getSubmittedAt());
    }

    @Test void reportsANewBestOnlyWhenItBeatsAnEarlierAttempt() {
        when(attempts.bestPercent("owner", 42L, "minna", 26)).thenReturn(50);
        var result = service.submit("owner", 42L, new Submission(26, List.of(new Answer(1, Kind.MEANING, "hội thao"))));
        assertEquals(100, result.bestPercent());
        assertTrue(result.newBest());
    }

    @Test void refusesAnswersFromAnotherLessonDuplicatesAndKanaReadings() {
        assertBad(new Submission(26, List.of(new Answer(3, Kind.MEANING, "nuôi"))));
        assertBad(new Submission(26, List.of(new Answer(1, Kind.MEANING, "a"), new Answer(1, Kind.MEANING, "b"))));
        assertBad(new Submission(26, List.of(new Answer(2, Kind.READING, "いつでも"))));
        assertBad(new Submission(99, List.of(new Answer(1, Kind.MEANING, "hội thao"))));
        assertBad(new Submission(26, List.of()));
        verify(attempts, never()).save(any());
    }

    @Test void checksCourseAccessBeforeGrading() {
        when(decks.get("stranger", 42L)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN));
        var error = assertThrows(ResponseStatusException.class, () ->
                service.submit("stranger", 42L, new Submission(26, List.of(new Answer(1, Kind.MEANING, "hội thao")))));
        assertEquals(403, error.getStatusCode().value());
        verify(attempts, never()).save(any());
    }

    @Test void summaryListsBestScorePerLessonOfTheCourseDeck() {
        var score = mock(VocabularyQuizAttemptRepository.UnitScore.class);
        when(score.getUnit()).thenReturn(26);
        when(score.getAttempts()).thenReturn(3L);
        when(score.getBestPercent()).thenReturn(90);
        when(attempts.scores("owner", 42L, "minna")).thenReturn(List.of(score));
        var summary = service.summary("owner", 42L);
        assertEquals("minna", summary.deckId());
        assertEquals(26, summary.units().get(0).unit());
        assertEquals(90, summary.units().get(0).bestPercent());
        assertEquals(3, summary.units().get(0).attempts());
    }

    private void assertBad(Submission submission) {
        var error = assertThrows(ResponseStatusException.class, () -> service.submit("owner", 42L, submission));
        assertEquals(400, error.getStatusCode().value());
    }
}
