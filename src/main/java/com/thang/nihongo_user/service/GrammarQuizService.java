package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.model.GrammarQuizAttempt;
import com.thang.nihongo_user.repository.GrammarQuizAttemptRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Per-lesson grammar quiz: Minna no Nihongo II (lessons 26–50) for N4 courses, TRY! N3 (lessons 1–21) for N3 courses.
 * Questions ask what a pattern is used for (USAGE) or which conjugated form fits (FORM); each lesson ends with one
 * MATCH question pairing every pattern of the lesson with its meaning. The banks ship with the
 * service; the server grades submissions.
 */
@Service
public class GrammarQuizService {
    /** Textbook level of the course → its question bank. */
    static final Map<String, String> BANK_PATHS = Map.of(
            "N4", "content/minna-2-grammar-quiz.json",
            "N3", "content/try-n3-grammar-quiz.json");

    /** One pattern of a MATCH question and the meaning it must be paired with. */
    public record Pair(String pattern, String meaning) {}
    /**
     * USAGE／FORM: one of four options. MATCH: options are the meanings, and the answer is the meanings in the order of
     * {@code pairs} joined with a line break; the learner's choice is built the same way.
     */
    public record Question(String id, int unit, String grammar, String kind, String prompt, String sentence,
                           String answer, List<String> options, String explanation, List<Pair> pairs) {}
    public record Lesson(int unit, String name) {}
    public record Bank(String id, String title, String level, int totalQuestions, List<Lesson> lessons,
                       List<Question> questions) {}
    public record Answer(String id, String choice) {}
    public record Submission(Integer unit, List<Answer> answers) {}
    public record Graded(String id, String grammar, String kind, String prompt, String sentence, String choice,
                         String expected, String explanation, boolean correct) {}
    public record Result(long attemptId, int unit, int total, int correct, int percent, int bestPercent,
                         boolean newBest, List<Graded> answers) {}
    public record UnitScore(int unit, long attempts, int bestPercent, LocalDateTime lastSubmittedAt) {}
    public record Summary(String bankId, List<UnitScore> units) {}

    private final VocabularyDeckService decks;
    private final GrammarQuizAttemptRepository attempts;
    private final Map<String, Bank> banks = new HashMap<>();
    private final Map<String, Map<String, Question>> questionsById = new HashMap<>();
    /** Same local wall-clock time as the other learning records of this service. */
    Clock clock = Clock.systemDefaultZone();

    public GrammarQuizService(VocabularyDeckService decks, GrammarQuizAttemptRepository attempts, ObjectMapper mapper)
            throws IOException {
        this.decks = decks;
        this.attempts = attempts;
        for (var entry : BANK_PATHS.entrySet()) {
            Bank bank;
            try (var input = new ClassPathResource(entry.getValue()).getInputStream()) {
                bank = mapper.readValue(input, Bank.class);
            }
            validate(bank);
            if (!entry.getKey().equalsIgnoreCase(bank.level()))
                throw new IllegalStateException("Bộ câu hỏi " + entry.getValue() + " không đúng cấp độ " + entry.getKey());
            banks.put(entry.getKey(), bank);
            questionsById.put(entry.getKey(),
                    bank.questions().stream().collect(Collectors.toMap(Question::id, Function.identity())));
        }
    }

    static void validate(Bank bank) {
        if (bank.questions() == null || bank.questions().isEmpty() || bank.totalQuestions() != bank.questions().size()
                || bank.lessons() == null || bank.lessons().isEmpty())
            throw new IllegalStateException("Bộ câu hỏi ngữ pháp " + bank.id() + " không hợp lệ");
        Set<Integer> units = bank.lessons().stream().map(Lesson::unit).collect(Collectors.toSet());
        Set<String> ids = new HashSet<>();
        for (var question : bank.questions()) {
            boolean valid = ids.add(question.id()) && units.contains(question.unit()) && question.options() != null;
            if (valid && "MATCH".equals(question.kind())) {
                var pairs = question.pairs();
                valid = pairs != null && pairs.size() >= 2 && question.options().size() == pairs.size()
                        && new HashSet<>(question.options()).size() == pairs.size()
                        && pairs.stream().map(Pair::meaning).collect(Collectors.toSet()).equals(new HashSet<>(question.options()))
                        && question.answer().equals(String.join("\n", pairs.stream().map(Pair::meaning).toList()));
            } else if (valid) {
                valid = Set.of("USAGE", "FORM").contains(question.kind()) && question.options().size() == 4
                        && new HashSet<>(question.options()).size() == 4 && question.options().contains(question.answer());
            }
            if (!valid) throw new IllegalStateException("Câu hỏi ngữ pháp không hợp lệ: " + question.id());
        }
    }

    /** The lesson questions, with answers and explanations so the learner gets feedback right after each choice. */
    @Transactional(readOnly = true)
    public Bank bank(String userId, Long courseId) {
        return bankFor(userId, courseId);
    }

    @Transactional(readOnly = true)
    public Summary summary(String userId, Long courseId) {
        var bank = bankFor(userId, courseId);
        var units = attempts.scores(userId, courseId, bank.id()).stream()
                .map(s -> new UnitScore(s.getUnit(), s.getAttempts(), s.getBestPercent(), s.getLastSubmittedAt()))
                .toList();
        return new Summary(bank.id(), units);
    }

    @Transactional
    public Result submit(String userId, Long courseId, Submission submission) {
        var bank = bankFor(userId, courseId);
        var byId = questionsById.get(bank.level().toUpperCase(Locale.ROOT));
        if (submission == null || submission.unit() == null) throw bad("Chọn bài cần kiểm tra.");
        int unit = submission.unit();
        long inUnit = bank.questions().stream().filter(q -> q.unit() == unit).count();
        if (inUnit == 0) throw bad("Bài này không có câu hỏi ngữ pháp.");
        var answers = submission.answers();
        if (answers == null || answers.isEmpty()) throw bad("Bài kiểm tra chưa có câu trả lời nào.");
        if (answers.size() > inUnit) throw bad("Số câu trả lời vượt quá số câu của bài.");

        Set<String> seen = new HashSet<>();
        List<Graded> graded = new ArrayList<>(answers.size());
        for (var answer : answers) {
            if (answer == null || answer.id() == null) throw bad("Câu trả lời không hợp lệ.");
            var question = byId.get(answer.id());
            if (question == null || question.unit() != unit) throw bad("Câu hỏi không thuộc bài đã chọn.");
            if (!seen.add(question.id())) throw bad("Một câu hỏi được trả lời hai lần.");
            String choice = answer.choice() == null ? "" : answer.choice().strip();
            if (choice.length() > 2000) throw bad("Câu trả lời quá dài.");
            graded.add(new Graded(question.id(), question.grammar(), question.kind(), question.prompt(), question.sentence(),
                    choice, question.answer(), question.explanation(), normalize(choice).equals(normalize(question.answer()))));
        }

        int correct = (int) graded.stream().filter(Graded::correct).count();
        int percent = correct * 100 / graded.size();
        int previousBest = attempts.bestPercent(userId, courseId, bank.id(), unit);
        var attempt = new GrammarQuizAttempt();
        attempt.setUserId(userId);
        attempt.setCourseId(courseId);
        attempt.setBankId(bank.id());
        attempt.setUnit(unit);
        attempt.setTotal(graded.size());
        attempt.setCorrect(correct);
        attempt.setPercent(percent);
        attempt.setSubmittedAt(LocalDateTime.now(clock));
        var saved = attempts.save(attempt);
        return new Result(saved.getId(), unit, graded.size(), correct, percent, Math.max(previousBest, percent),
                previousBest >= 0 && percent > previousBest, graded);
    }

    /** Uses the vocabulary deck lookup for the course checks: active course, active subscription, textbook level. */
    private Bank bankFor(String userId, Long courseId) {
        var deck = decks.get(userId, courseId);
        var bank = deck.level() == null ? null : banks.get(deck.level().toUpperCase(Locale.ROOT));
        if (bank == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khóa học này chưa có bài kiểm tra ngữ pháp");
        return bank;
    }

    private static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFKC).strip();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
