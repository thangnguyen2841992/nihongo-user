package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.VocabularyQuizAttempt;
import com.thang.nihongo_user.repository.VocabularyQuizAttemptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Per-lesson vocabulary quiz. The browser builds the multiple-choice questions from the deck, but only sends the
 * chosen answers: the server grades them against the deck, so a stored score is always one the learner earned.
 */
@Service
@RequiredArgsConstructor
public class VocabularyQuizService {
    /** MEANING: Japanese word → Vietnamese meaning. READING: kanji word → its kana reading. */
    public enum Kind { MEANING, READING }

    public record Answer(Integer number, Kind kind, String choice) {}
    public record Submission(Integer unit, List<Answer> answers) {}
    public record Graded(int number, Kind kind, String word, String choice, String expected, boolean correct) {}
    public record Result(long attemptId, int unit, int total, int correct, int percent, int bestPercent,
                         boolean newBest, List<Graded> answers) {}
    public record UnitScore(int unit, long attempts, int bestPercent, LocalDateTime lastSubmittedAt) {}
    public record Summary(String deckId, List<UnitScore> units) {}

    private final VocabularyDeckService decks;
    private final VocabularyQuizAttemptRepository attempts;
    /** Same local wall-clock time as the other learning records of this service. */
    Clock clock = Clock.systemDefaultZone();

    @Transactional(readOnly = true)
    public Summary summary(String userId, Long courseId) {
        var deck = decks.get(userId, courseId);
        var units = attempts.scores(userId, courseId, deck.id()).stream()
                .map(s -> new UnitScore(s.getUnit(), s.getAttempts(), s.getBestPercent(), s.getLastSubmittedAt()))
                .toList();
        return new Summary(deck.id(), units);
    }

    @Transactional
    public Result submit(String userId, Long courseId, Submission submission) {
        var deck = decks.get(userId, courseId);
        if (submission == null || submission.unit() == null) throw bad("Chọn bài cần kiểm tra.");
        int unit = submission.unit();
        Map<Integer, VocabularyDeckService.Card> cards = deck.cards().stream()
                .filter(card -> card.unit() == unit)
                .collect(Collectors.toMap(VocabularyDeckService.Card::number, Function.identity(), (a, b) -> a));
        if (cards.isEmpty()) throw bad("Bài này không có trong bộ từ vựng của khóa học.");
        var answers = submission.answers();
        if (answers == null || answers.isEmpty()) throw bad("Bài kiểm tra chưa có câu trả lời nào.");
        if (answers.size() > cards.size() * Kind.values().length) throw bad("Số câu trả lời vượt quá số câu của bài.");

        Set<String> seen = new HashSet<>();
        List<Graded> graded = new ArrayList<>(answers.size());
        for (var answer : answers) {
            if (answer == null || answer.number() == null || answer.kind() == null)
                throw bad("Câu trả lời không hợp lệ.");
            var card = cards.get(answer.number());
            if (card == null) throw bad("Câu hỏi không thuộc bài đã chọn.");
            if (!seen.add(answer.number() + ":" + answer.kind())) throw bad("Một câu hỏi được trả lời hai lần.");
            String expected = answer.kind() == Kind.MEANING ? card.meaning() : card.reading();
            if (answer.kind() == Kind.READING && !hasReading(card)) throw bad("Từ này không có câu hỏi cách đọc.");
            String choice = answer.choice() == null ? "" : answer.choice().strip();
            if (choice.length() > 300) throw bad("Câu trả lời quá dài.");
            graded.add(new Graded(card.number(), answer.kind(), card.word(), choice, expected,
                    normalize(choice).equals(normalize(expected))));
        }

        int correct = (int) graded.stream().filter(Graded::correct).count();
        int percent = correct * 100 / graded.size();
        int previousBest = attempts.bestPercent(userId, courseId, deck.id(), unit);
        var attempt = new VocabularyQuizAttempt();
        attempt.setUserId(userId);
        attempt.setCourseId(courseId);
        attempt.setDeckId(deck.id());
        attempt.setUnit(unit);
        attempt.setTotal(graded.size());
        attempt.setCorrect(correct);
        attempt.setPercent(percent);
        attempt.setSubmittedAt(LocalDateTime.now(clock));
        var saved = attempts.save(attempt);
        return new Result(saved.getId(), unit, graded.size(), correct, percent, Math.max(previousBest, percent),
                previousBest >= 0 && percent > previousBest, graded);
    }

    /** A reading question only makes sense when the word is written with kanji and the deck gives its kana. */
    static boolean hasReading(VocabularyDeckService.Card card) {
        return card.reading() != null && !card.reading().isBlank() && !card.reading().strip().equals(card.word().strip());
    }

    private static String normalize(String value) {
        return value == null ? "" : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC).strip();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
