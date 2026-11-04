package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.model.GrammarQuizAttempt;
import com.thang.nihongo_user.repository.GrammarQuizAttemptRepository;
import com.thang.nihongo_user.service.GrammarQuizService.Answer;
import com.thang.nihongo_user.service.GrammarQuizService.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GrammarQuizServiceTest {
    private final VocabularyDeckService decks = mock(VocabularyDeckService.class);
    private final GrammarQuizAttemptRepository attempts = mock(GrammarQuizAttemptRepository.class);
    private final GrammarQuizService service;

    GrammarQuizServiceTest() throws IOException {
        service = new GrammarQuizService(decks, attempts, new ObjectMapper());
    }

    private static VocabularyDeckService.Deck deck(String level) {
        return new VocabularyDeckService.Deck("deck", "Deck", level, 0, List.of());
    }

    @BeforeEach void setUp() {
        service.clock = Clock.fixed(Instant.parse("2026-11-04T01:00:00Z"), ZoneOffset.UTC);
        when(decks.get("owner", 42L)).thenReturn(deck("N4"));
        when(attempts.bestPercent(any(), any(), any(), anyInt())).thenReturn(-1);
        when(attempts.save(any())).thenAnswer(invocation -> {
            GrammarQuizAttempt attempt = invocation.getArgument(0);
            attempt.setId(5L);
            return attempt;
        });
    }

    @Test void bundledBankCoversEveryLessonOfMinnaTwoWithValidQuestions() {
        var bank = service.bank("owner", 42L);
        assertEquals("N4", bank.level());
        assertEquals(25, bank.lessons().size());
        assertEquals(bank.totalQuestions(), bank.questions().size());
        var units = bank.questions().stream().map(GrammarQuizService.Question::unit).collect(Collectors.toSet());
        for (int unit = 26; unit <= 50; unit++) assertTrue(units.contains(unit), "missing lesson " + unit);
        assertTrue(bank.questions().stream().filter(q -> !q.kind().equals("MATCH")).allMatch(q -> q.options().size() == 4
                && q.options().contains(q.answer()) && !q.explanation().isBlank()
                && (!q.kind().equals("FORM") || q.sentence().contains("＿＿"))));
        // Every lesson ends with one MATCH question covering all of its patterns.
        for (int unit = 26; unit <= 50; unit++) {
            int u = unit;
            assertEquals(1, bank.questions().stream().filter(q -> q.unit() == u && q.kind().equals("MATCH")).count());
        }
    }

    @Test void gradesAgainstTheBankAndStoresTheScore() {
        var lesson = service.bank("owner", 42L).questions().stream().filter(q -> q.unit() == 26).toList();
        var first = lesson.get(0);
        var second = lesson.get(1);
        String wrong = second.options().stream().filter(o -> !o.equals(second.answer())).findFirst().orElseThrow();
        var result = service.submit("owner", 42L, new Submission(26, List.of(
                new Answer(first.id(), first.answer()), new Answer(second.id(), wrong))));

        assertEquals(2, result.total());
        assertEquals(1, result.correct());
        assertEquals(50, result.percent());
        assertFalse(result.answers().get(1).correct());
        assertEquals(second.answer(), result.answers().get(1).expected());
        assertFalse(result.answers().get(1).explanation().isBlank());
        var saved = ArgumentCaptor.forClass(GrammarQuizAttempt.class);
        verify(attempts).save(saved.capture());
        assertEquals("minna-no-nihongo-2-grammar", saved.getValue().getBankId());
        assertEquals(26, saved.getValue().getUnit());
        assertEquals(50, saved.getValue().getPercent());
    }

    @Test void gradesTheMatchQuestionOnlyWhenEveryPairIsRight() {
        var match = service.bank("owner", 42L).questions().stream()
                .filter(q -> q.unit() == 26 && q.kind().equals("MATCH")).findFirst().orElseThrow();
        assertEquals(3, match.pairs().size());
        var meanings = match.pairs().stream().map(GrammarQuizService.Pair::meaning).toList();
        var swapped = String.join("\n", List.of(meanings.get(1), meanings.get(0), meanings.get(2)));
        var right = service.submit("owner", 42L, new Submission(26, List.of(new Answer(match.id(), String.join("\n", meanings)))));
        assertEquals(100, right.percent());
        var wrong = service.submit("owner", 42L, new Submission(26, List.of(new Answer(match.id(), swapped))));
        assertEquals(0, wrong.percent());
        assertEquals(match.answer(), wrong.answers().get(0).expected());
    }

    @Test void refusesQuestionsOfAnotherLessonAndDuplicates() {
        var bank = service.bank("owner", 42L).questions();
        var of27 = bank.stream().filter(q -> q.unit() == 27).findFirst().orElseThrow();
        var of26 = bank.stream().filter(q -> q.unit() == 26).findFirst().orElseThrow();
        assertBad(new Submission(26, List.of(new Answer(of27.id(), of27.answer()))));
        assertBad(new Submission(26, List.of(new Answer(of26.id(), "a"), new Answer(of26.id(), "b"))));
        assertBad(new Submission(26, List.of(new Answer("nope", "a"))));
        assertBad(new Submission(26, List.of()));
        verify(attempts, never()).save(any());
    }

    @Test void n3CoursesGetTheTryN3BankWithItsOwnScores() {
        when(decks.get("owner", 7L)).thenReturn(deck("N3"));
        var bank = service.bank("owner", 7L);
        assertEquals("try-n3-grammar", bank.id());
        assertEquals(21, bank.lessons().size());
        assertEquals("Lần đầu leo núi Phú Sĩ (1)", bank.lessons().get(0).name());
        var units = bank.questions().stream().map(GrammarQuizService.Question::unit).collect(Collectors.toSet());
        for (int unit = 1; unit <= 21; unit++) assertTrue(units.contains(unit), "missing lesson " + unit);

        var first = bank.questions().stream().filter(q -> q.unit() == 1).findFirst().orElseThrow();
        var result = service.submit("owner", 7L, new Submission(1, List.of(new Answer(first.id(), first.answer()))));
        assertEquals(100, result.percent());
        var saved = ArgumentCaptor.forClass(GrammarQuizAttempt.class);
        verify(attempts).save(saved.capture());
        assertEquals("try-n3-grammar", saved.getValue().getBankId());
        // A Minna question is not part of the N3 bank.
        var minna = service.bank("owner", 42L).questions().get(0);
        assertBadFor(7L, new Submission(1, List.of(new Answer(minna.id(), minna.answer()))));
    }

    @Test void coursesWithoutAGrammarBankAndLearnersWithoutAccessAreRefused() {
        when(decks.get("owner", 9L)).thenReturn(deck("N5"));
        var error = assertThrows(ResponseStatusException.class, () -> service.bank("owner", 9L));
        assertEquals(404, error.getStatusCode().value());
        when(decks.get("stranger", 42L)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.summary("stranger", 42L))
                .getStatusCode().value());
    }

    private void assertBad(Submission submission) {
        assertBadFor(42L, submission);
    }

    private void assertBadFor(Long courseId, Submission submission) {
        var error = assertThrows(ResponseStatusException.class, () -> service.submit("owner", courseId, submission));
        assertEquals(400, error.getStatusCode().value());
    }
}
