package com.thang.nihongo_user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.model.EnglishStudyProgress;
import com.thang.nihongo_user.repository.EnglishStudyProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Stores the English self-study notebook as the learner's own JSON document. The content is only ever returned to
 * the same learner; the server checks its size and shape and uses a revision number so that two devices editing at
 * the same time get a conflict (and merge on the client) instead of silently overwriting each other.
 */
@Service @RequiredArgsConstructor
public class EnglishStudyService {
    /** Three notebooks of 20,000 characters each, plus progress, comfortably fit; anything larger is rejected. */
    public static final int MAX_BYTES = 256 * 1024;
    private final EnglishStudyProgressRepository rows;
    private final ObjectMapper json;

    public record Snapshot(long version, long updatedAt, JsonNode data) {}

    public static class Conflict extends RuntimeException {
        private final transient Snapshot current;
        public Conflict(Snapshot current) { super("English study notebook changed on another device"); this.current = current; }
        public Snapshot current() { return current; }
    }

    @Transactional(readOnly = true)
    public Optional<Snapshot> get(String userId) {
        return rows.findById(userId).map(this::snapshot);
    }

    @Transactional
    public Snapshot save(String userId, long expectedVersion, JsonNode data) {
        if (data == null || !data.isObject())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu sổ tay không hợp lệ");
        String payload;
        try { payload = json.writeValueAsString(data); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dữ liệu sổ tay không hợp lệ"); }
        if (payload.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Sổ tay quá lớn để đồng bộ");
        var row = rows.lock(userId).orElse(null);
        if (row == null) {
            if (expectedVersion != 0) throw new Conflict(null);
            row = new EnglishStudyProgress();
            row.setUserId(userId);
        } else if (row.getRevision() != expectedVersion) {
            throw new Conflict(snapshot(row));
        }
        row.setPayload(payload);
        row.setRevision(row.getRevision() + 1);
        row.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        // A first save racing another first save fails on the primary key and is reported as a conflict.
        return snapshot(rows.saveAndFlush(row));
    }

    private Snapshot snapshot(EnglishStudyProgress row) {
        try {
            return new Snapshot(row.getRevision(), row.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli(), json.readTree(row.getPayload()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored English study notebook is not JSON", e);
        }
    }
}
