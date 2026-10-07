package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.StudyCard;
import com.thang.nihongo_user.repository.StudyCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor
public class StudyService {
    private final StudyCardRepository cards;

    public record CardInput(String sourceKey, String kind, Long courseId, Long bookId, Long lessonId,
                            Long grammarId, Long exampleId, String front, String back, String note) {}

    public List<StudyCard> all(String userId) { return cards.findByUserIdOrderByCreatedAtDesc(userId); }
    public List<StudyCard> due(String userId) {
        return cards.findByUserIdAndDueAtLessThanEqualOrderByDueAtAsc(userId, LocalDateTime.now());
    }

    @Transactional
    public StudyCard save(String userId, CardInput input) {
        if (input == null || input.kind() == null || !List.of("EXAMPLE", "GRAMMAR", "VOCAB").contains(input.kind())
                || input.sourceKey() == null || !input.sourceKey().matches("(example|grammar):[1-9][0-9]*|vocab:[a-zA-Z0-9-]{1,50}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nguồn thẻ không hợp lệ");
        if (input.kind().equals("EXAMPLE") && !input.sourceKey().equals("example:" + input.exampleId())
                || input.kind().equals("GRAMMAR") && !input.sourceKey().equals("grammar:" + input.grammarId())
                || input.kind().equals("VOCAB") && !input.sourceKey().startsWith("vocab:"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nguồn thẻ không khớp");
        String front = required(input.front(), 2000);
        String back = optional(input.back(), 2000);
        String note = optional(input.note(), 2000);
        StudyCard card = cards.findByUserIdAndSourceKey(userId, input.sourceKey()).orElseGet(() -> {
            StudyCard created = new StudyCard();
            created.setUserId(userId);
            created.setSourceKey(input.sourceKey());
            created.setCreatedAt(LocalDateTime.now());
            created.setDueAt(LocalDateTime.now());
            return created;
        });
        card.setKind(input.kind());
        card.setCourseId(input.courseId()); card.setBookId(input.bookId()); card.setLessonId(input.lessonId());
        card.setGrammarId(input.grammarId()); card.setExampleId(input.exampleId());
        card.setFront(front); card.setBack(back); card.setNote(note);
        return cards.save(card);
    }

    @Transactional
    public StudyCard review(String userId, Long id, boolean remembered) {
        StudyCard card = own(userId, id);
        int repetitions = remembered ? card.getRepetitions() + 1 : 0;
        card.setRepetitions(repetitions);
        int[] days = {1, 3, 7, 14, 30};
        card.setDueAt(remembered ? LocalDateTime.now().plusDays(days[Math.min(repetitions - 1, days.length - 1)])
                : LocalDateTime.now().plusMinutes(10));
        return cards.save(card);
    }

    @Transactional
    public void delete(String userId, Long id) { cards.delete(own(userId, id)); }

    private StudyCard own(String userId, Long id) {
        return cards.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thẻ"));
    }
    private String required(String value, int max) {
        String result = optional(value, max);
        if (result == null || result.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung thẻ không được trống");
        return result;
    }
    private String optional(String value, int max) {
        if (value == null) return null;
        String result = value.trim();
        if (result.length() > max) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung quá dài");
        return result;
    }
}
