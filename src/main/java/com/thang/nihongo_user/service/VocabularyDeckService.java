package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.model.CourseStatus;
import com.thang.nihongo_user.repository.ICourseRepository;
import com.thang.nihongo_user.repository.IUserSubscriptionRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Service
public class VocabularyDeckService {
    private final ICourseRepository courses;
    private final IUserSubscriptionRepository subscriptions;
    private final Deck mimiKaraN3;

    public VocabularyDeckService(ICourseRepository courses, IUserSubscriptionRepository subscriptions,
                                 ObjectMapper mapper) throws IOException {
        this.courses = courses;
        this.subscriptions = subscriptions;
        try (var input = new ClassPathResource("content/mimi-kara-n3-vocabulary.json").getInputStream()) {
            this.mimiKaraN3 = mapper.readValue(input, Deck.class);
        }
        if (mimiKaraN3.cards() == null || mimiKaraN3.cards().size() != 880
                || mimiKaraN3.totalCards() != mimiKaraN3.cards().size()) {
            throw new IllegalStateException("Bộ flashcard Mimi Kara N3 không đủ 880 thẻ");
        }
        if (mimiKaraN3.cards().stream().anyMatch(card ->
                card.exampleJapanese() == null || card.exampleJapanese().isBlank()
                        || card.exampleVietnamese() == null || card.exampleVietnamese().isBlank())) {
            throw new IllegalStateException("Bộ flashcard Mimi Kara N3 có thẻ thiếu câu ví dụ");
        }
    }

    public Deck get(String userId, Long courseId) {
        var course = courses.findById(courseId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khóa học"));
        if (course.getActive() != CourseStatus.ACTIVE || !Long.valueOf(3).equals(course.getLevelId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Khóa học này không có bộ flashcard N3");
        }
        if (!subscriptions.existsActive(userId, courseId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn cần khóa học N3 còn hạn để học bộ flashcard này");
        }
        return mimiKaraN3;
    }

    public record Card(int number, int unit, String word, String reading, String meaning,
                       String sinoVietnamese, String exampleJapanese, String exampleVietnamese) {}
    public record Deck(String id, String title, String level, int totalCards, List<Card> cards) {}
}
