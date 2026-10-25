package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.model.EnglishStudyProgress;
import com.thang.nihongo_user.repository.EnglishStudyProgressRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:english_study;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.cloud.discovery.enabled=false", "eureka.client.enabled=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = EnglishStudyServiceTest.Config.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EnglishStudyServiceTest {
    @TestConfiguration @EntityScan(basePackageClasses = EnglishStudyProgress.class)
    @EnableJpaRepositories(basePackageClasses = EnglishStudyProgressRepository.class)
    @Import(EnglishStudyService.class)
    static class Config { @Bean ObjectMapper mapper() { return new ObjectMapper(); } }

    @Autowired EnglishStudyService study;
    final ObjectMapper json = new ObjectMapper();
    String owner;
    @BeforeEach void owner() { owner = UUID.randomUUID().toString(); }

    @Test void savesNotebookAndRejectsAStaleDeviceWithTheCurrentCopy() throws Exception {
        assertTrue(study.get(owner).isEmpty());
        var first = study.save(owner, 0, json.readTree("{\"essential-grammar\":{\"notes\":\"phone\"}}"));
        assertEquals(1, first.version());
        var second = study.save(owner, 1, json.readTree("{\"essential-grammar\":{\"notes\":\"laptop\"}}"));
        assertEquals(2, second.version());
        var conflict = assertThrows(EnglishStudyService.Conflict.class,
                () -> study.save(owner, 1, json.readTree("{\"essential-grammar\":{\"notes\":\"stale\"}}")));
        assertEquals("laptop", conflict.current().data().path("essential-grammar").path("notes").asText());
        assertEquals("laptop", study.get(owner).orElseThrow().data().path("essential-grammar").path("notes").asText());
        assertThrows(EnglishStudyService.Conflict.class, () -> study.save(UUID.randomUUID().toString(), 3, json.readTree("{}")));
    }

    @Test void rejectsNonObjectsAndOversizedNotebooks() throws Exception {
        assertThrows(ResponseStatusException.class, () -> study.save(owner, 0, json.readTree("[1,2]")));
        var big = json.createObjectNode().put("notes", "x".repeat(EnglishStudyService.MAX_BYTES));
        var error = assertThrows(ResponseStatusException.class, () -> study.save(owner, 0, big));
        assertEquals(413, error.getStatusCode().value());
        assertTrue(study.get(owner).isEmpty());
    }
}
