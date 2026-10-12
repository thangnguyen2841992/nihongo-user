package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.CourseStatus;
import com.thang.nihongo_user.repository.ICourseRepository;
import com.thang.nihongo_user.repository.IUserSubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VocabularyDeckServiceTest {
    private final ICourseRepository courses = mock(ICourseRepository.class);
    private final IUserSubscriptionRepository subscriptions = mock(IUserSubscriptionRepository.class);
    private final VocabularyDeckService service;

    VocabularyDeckServiceTest() throws IOException {
        service = new VocabularyDeckService(courses, subscriptions, new ObjectMapper());
    }

    @Test void activeN3LearnerGetsAllCardsInBookOrder() {
        when(courses.findById(42L)).thenReturn(Optional.of(n3()));
        when(subscriptions.existsActive("owner", 42L)).thenReturn(true);
        var deck = service.get("owner", 42L);
        assertEquals(880, deck.cards().size());
        assertEquals(1, deck.cards().get(0).number());
        assertEquals("男性", deck.cards().get(0).word());
        assertEquals("NAM TÍNH", deck.cards().get(0).sinoVietnamese());
        assertFalse(deck.cards().get(0).exampleJapanese().isBlank());
        assertFalse(deck.cards().get(0).exampleVietnamese().isBlank());
        assertTrue(deck.cards().stream().allMatch(card ->
                !card.exampleJapanese().isBlank() && !card.exampleVietnamese().isBlank()));
        assertEquals(12, deck.cards().get(879).unit());
        assertEquals(880, deck.cards().get(879).number());
    }

    @Test void refusesLearnerWithoutAnActiveSubscription() {
        when(courses.findById(42L)).thenReturn(Optional.of(n3()));
        var error = assertThrows(ResponseStatusException.class, () -> service.get("owner", 42L));
        assertEquals(403, error.getStatusCode().value());
    }

    @Test void refusesANonN3CourseEvenWhenSubscribed() {
        var course = n3(); course.setLevelId(4L);
        when(courses.findById(42L)).thenReturn(Optional.of(course));
        when(subscriptions.existsActive("owner", 42L)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class, () -> service.get("owner", 42L));
        assertEquals(404, error.getStatusCode().value());
    }

    private Course n3() {
        return Course.builder().courseId(42L).levelId(3L).active(CourseStatus.ACTIVE).build();
    }
}
