package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.CourseStatus;
import com.thang.nihongo_user.repository.ICourseRepository;
import com.thang.nihongo_user.repository.IStaffClient;
import com.thang.nihongo_user.repository.IUserSubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Vocabulary flashcards of a course's textbook: Mimi Kara Oboeru N3 for N3 courses, Minna no Nihongo Shokyu II
 * (lessons 26-50, one unit per lesson) for N4 courses.
 */
@Service
@Slf4j
public class VocabularyDeckService {
    private static final Pattern N4_NAME = Pattern.compile("(?i)\\bN\\s*4\\b");
    private final ICourseRepository courses;
    private final IUserSubscriptionRepository subscriptions;
    private final IStaffClient staff;
    private final Deck mimiKaraN3;
    private final Deck minnaN4;

    public VocabularyDeckService(ICourseRepository courses, IUserSubscriptionRepository subscriptions,
                                 IStaffClient staff, ObjectMapper mapper) throws IOException {
        this.courses = courses;
        this.subscriptions = subscriptions;
        this.staff = staff;
        this.mimiKaraN3 = load(mapper, "content/mimi-kara-n3-vocabulary.json");
        this.minnaN4 = load(mapper, "content/minna-2-vocabulary.json");
        if (mimiKaraN3.cards() == null || mimiKaraN3.cards().size() != 880
                || mimiKaraN3.totalCards() != mimiKaraN3.cards().size()) {
            throw new IllegalStateException("Bộ flashcard Mimi Kara N3 không đủ 880 thẻ");
        }
        if (mimiKaraN3.cards().stream().anyMatch(card ->
                card.exampleJapanese() == null || card.exampleJapanese().isBlank()
                        || card.exampleVietnamese() == null || card.exampleVietnamese().isBlank())) {
            throw new IllegalStateException("Bộ flashcard Mimi Kara N3 có thẻ thiếu câu ví dụ");
        }
        if (minnaN4.cards() == null || minnaN4.cards().size() < 800 || minnaN4.totalCards() != minnaN4.cards().size()
                || minnaN4.cards().stream().anyMatch(card -> card.unit() < 26 || card.unit() > 50
                || card.word() == null || card.word().isBlank() || card.meaning() == null || card.meaning().isBlank())) {
            throw new IllegalStateException("Bộ flashcard Minna no Nihongo II không hợp lệ");
        }
    }

    private static Deck load(ObjectMapper mapper, String path) throws IOException {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(input, Deck.class);
        }
    }

    public Deck get(String userId, Long courseId) {
        var course = courses.findById(courseId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học"));
        if (course.getActive() != CourseStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học");
        }
        var deck = deckFor(course);
        if (deck == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khóa học này chưa có bộ flashcard từ vựng");
        }
        if (!subscriptions.existsActive(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn cần khóa học còn hạn để học bộ flashcard này");
        }
        return deck;
    }

    private Deck deckFor(Course course) {
        if (Long.valueOf(3).equals(course.getLevelId())) return mimiKaraN3;
        String name = course.getCourseName() == null ? "" : Normalizer.normalize(course.getCourseName(), Normalizer.Form.NFKC);
        if (N4_NAME.matcher(name).find()) return minnaN4;
        if (course.getLevelId() == null) return null;
        try {
            // The level id behind "N4" is data; the course's books carry the level name.
            var books = staff.getBooksByLevel(course.getLevelId());
            if (books != null && books.stream().anyMatch(book ->
                    book.getLevelName() != null && book.getLevelName().trim().toUpperCase(Locale.ROOT).equals("N4"))) {
                return minnaN4;
            }
        } catch (RuntimeException unavailable) {
            log.warn("Cannot read the level of course {}: {}", course.getCourseId(), unavailable.toString());
        }
        return null;
    }

    public record Card(int number, int unit, String word, String reading, String meaning,
                       String sinoVietnamese, String exampleJapanese, String exampleVietnamese) {}
    public record Deck(String id, String title, String level, int totalCards, List<Card> cards) {}
}
