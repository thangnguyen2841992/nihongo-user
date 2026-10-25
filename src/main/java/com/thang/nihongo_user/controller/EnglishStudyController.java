package com.thang.nihongo_user.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.thang.nihongo_user.service.EnglishStudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nihongo-user/english-study")
@PreAuthorize("hasAnyRole('ADMIN','STAFF','USER')")
@RequiredArgsConstructor
public class EnglishStudyController {
    private final EnglishStudyService study;

    public record SaveRequest(long version, JsonNode data) {}
    public record ConflictResponse(String message, EnglishStudyService.Snapshot current) {}

    @GetMapping
    public ResponseEntity<EnglishStudyService.Snapshot> get(@AuthenticationPrincipal Jwt jwt) {
        return study.get(jwt.getSubject()).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public ResponseEntity<?> save(@AuthenticationPrincipal Jwt jwt, @RequestBody SaveRequest request) {
        if (request == null) return ResponseEntity.badRequest().build();
        try {
            return ResponseEntity.ok(study.save(jwt.getSubject(), request.version(), request.data()));
        } catch (EnglishStudyService.Conflict conflict) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ConflictResponse("Sổ tay đã thay đổi trên thiết bị khác", conflict.current()));
        } catch (DataIntegrityViolationException race) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ConflictResponse("Sổ tay đã thay đổi trên thiết bị khác", study.get(jwt.getSubject()).orElse(null)));
        }
    }
}
