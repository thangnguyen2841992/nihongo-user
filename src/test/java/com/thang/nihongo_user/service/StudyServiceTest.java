package com.thang.nihongo_user.service;

import com.thang.nihongo_user.model.StudyCard;
import com.thang.nihongo_user.repository.StudyCardRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StudyServiceTest {
    private final StudyCardRepository cards = mock(StudyCardRepository.class);
    private final StudyService service = new StudyService(cards);

    @Test void savesOneCardPerSourceAndPreservesReviewScheduleOnNoteEdit() {
        when(cards.save(any())).thenAnswer(call -> call.getArgument(0));
        var input = new StudyService.CardInput("example:7", "EXAMPLE", 1L, 2L, 3L, 4L, 7L, "こんにちは", "Xin chào", null);
        StudyCard first = service.save("owner", input);
        assertEquals("owner", first.getUserId());
        assertNotNull(first.getDueAt());
        first.setRepetitions(2);
        first.setDueAt(LocalDateTime.now().plusDays(3));
        when(cards.findByUserIdAndSourceKey("owner", "example:7")).thenReturn(Optional.of(first));
        StudyCard edited = service.save("owner", new StudyService.CardInput("example:7", "EXAMPLE", 1L, 2L, 3L, 4L, 7L, "こんにちは", "Xin chào", "My note"));
        assertEquals(2, edited.getRepetitions());
        assertEquals(first.getDueAt(), edited.getDueAt());
        assertEquals("My note", edited.getNote());
    }

    @Test void reviewAndDeleteAreScopedToOwner() {
        StudyCard card = new StudyCard(); card.setId(8L); card.setUserId("owner");
        when(cards.findByIdAndUserId(8L, "owner")).thenReturn(Optional.of(card));
        when(cards.save(any())).thenAnswer(call -> call.getArgument(0));
        var remembered = service.review("owner", 8L, true);
        assertEquals(1, remembered.getRepetitions());
        assertTrue(remembered.getDueAt().isAfter(LocalDateTime.now().plusHours(23)));
        var forgotten = service.review("owner", 8L, false);
        assertEquals(0, forgotten.getRepetitions());
        assertTrue(forgotten.getDueAt().isBefore(LocalDateTime.now().plusMinutes(11)));
        assertThrows(ResponseStatusException.class, () -> service.delete("other", 8L));
        verify(cards, never()).delete(any());
    }
}
