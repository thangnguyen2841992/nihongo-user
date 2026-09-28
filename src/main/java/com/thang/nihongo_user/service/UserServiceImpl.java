package com.thang.nihongo_user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.model.Course;
import com.thang.nihongo_user.model.CoursePackage;
import com.thang.nihongo_user.model.UserExerciseAttempt;
import com.thang.nihongo_user.model.UserSubscription;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final ICourseRepository courseRepository;
    private final ICoursePackageRepository coursePackageRepository;
    private final IUserSubscriptionRepository subscriptionRepository;
    private final IStaffClient staffClient;
    private final IUserClient userClient;
    private final IUserExerciseAttemptRepository userExerciseAttemptRepository;
    private final WebClient geminiWebClient;
    private final ObjectMapper objectMapper;
    @Value("${gemini.model}")
    private String model;
    @Value("${gemini.api-key:}") private String geminiKey;
    // ================= COURSE =================

    @Override
    @Transactional
    public CourseDTO createNewCourse(Course course) {
        return mappingCourseToDTO(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDTO> getAllCourse() {
        return courseRepository.findAll().stream().map(this::mappingCourseToDTO).toList();
    }

    @Override
    public List<Long> findCourseIdsByUserId(String userId) {
        return subscriptionRepository.findByUserId(userId).stream().map(UserSubscription::getCourseId).distinct().toList();
    }

    @Override
    public boolean hasActiveSubscription(String userId, Long courseId) {
        return subscriptionRepository.existsActive(userId, courseId);
    }

    @Override
    public List<MyCourseDTO> findMyCourses(String userId) {

        return subscriptionRepository.findByUserId(userId).stream().map(sub -> {

            Course course = courseRepository.findById(sub.getCourseId()).orElseThrow(() -> new RuntimeException("Course not found"));

            CoursePackage pack = coursePackageRepository.findById(sub.getPackageId()).orElseThrow(() -> new RuntimeException("Package not found"));

            return MyCourseDTO.builder().courseId(course.getCourseId()).courseName(course.getCourseName()).packageName(pack.getPackageName()).progress(sub.getProgress()).enrolledAt(sub.getCreatedAt())   // ✅ FIX
                    .expiredAt(sub.getExpiredAt())    // ✅ FIX
                    .build();
        }).toList();
    }

    @Override
    public List<LessonResultResponse> getMyResults(String userId) {
        return this.userExerciseAttemptRepository.findByUserIdOrderBySubmittedAtDesc(userId).stream().map(this::convert).toList();
    }

    @Override
    public List<LessonResultResponse> getLessonResults(String userId, Long lessonId) {
        return this.userExerciseAttemptRepository.findByUserIdAndLessonIdOrderBySubmittedAtDesc(userId, lessonId).stream().map(this::convert).toList();
    }

    @Override
    public LessonResultResponse convert(UserExerciseAttempt entity) {
        String lessonName = "Bài học " + entity.getLessonId();
        try {
            lessonName = staffClient.getLessonById(entity.getLessonId()).getName();
        } catch (feign.FeignException e) {
            // An expired subscription does not remove ownership of past results.
            if (e.status() != 403 && e.status() != 404) throw e;
        }
        return LessonResultResponse.builder().resultId(entity.getUserExerciseAttemptId()).lessonId(entity.getLessonId()).lessonName(lessonName).totalQuestion(entity.getTotalQuestion()).correctCount(entity.getCorrectCount()).wrongCount(entity.getWrongCount()).score(entity.getScore()).submittedAt(entity.getSubmittedAt()).build();
    }

    @Override
    public Mono<JapaneseAiResponse> analyzeJapanese(String text) {
        if (text == null || text.isBlank() || text.length() > 2000) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nhập từ hoặc câu không quá 2.000 ký tự."));
        }
        text = text.trim();
        if (geminiKey == null || geminiKey.isBlank()) return Mono.error(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Chức năng AI chưa được cấu hình"));

        Map<String, Object> request = new HashMap<>();

        Map<String, Object> part =
                Map.of(
                        "text",
                        """
                        You are an expert Japanese language teacher specializing
                        in teaching Japanese to Vietnamese students.
        
                        Analyze the user's input and return the result strictly
                        according to the provided JSON schema.
        
                        =========================================================
                        1. INPUT HANDLING
                        =========================================================
        
                        The user may enter:
                        - Japanese word
                        - Japanese phrase
                        - Japanese sentence
                        - Multiple Japanese words
                        - Vietnamese text
                        - Japanese mixed with Vietnamese
        
                        IMPORTANT:
        
                        If the user input is Vietnamese only:
                        1. Translate the Vietnamese into natural Japanese.
                        2. Use the translated Japanese as the text to analyze.
                        3. Put the translated Japanese in "originalText".
                        4. Analyze vocabulary, grammar, reading and structure
                           based on the translated Japanese.
        
                        Example:
        
                        User:
                        Tôi hôm qua đã đi học.
        
                        originalText:
                        私は昨日学校に行きました。
        
                        translation:
                        Hôm qua tôi đã đi học.
        
                        If the input is Japanese:
                        analyze the Japanese directly.
        
                        If the input contains both Japanese and Vietnamese:
                        - Identify the Japanese part that should be analyzed.
                        - Use Vietnamese as context or instruction.
                        - Do not treat Vietnamese as Japanese vocabulary.
        
                        =========================================================
                        2. ORIGINAL TEXT
                        =========================================================
        
                        "originalText" must contain the Japanese text being analyzed.
        
                        For Japanese input:
                        preserve the user's Japanese exactly.
        
                        For Vietnamese input:
                        put the natural Japanese translation here.
        
                        NEVER unnecessarily replace Kanji with Hiragana.
                        NEVER use Romaji in originalText.
        
                        =========================================================
                        3. READING
                        =========================================================
        
                        "reading" is the Japanese pronunciation of originalText.
        
                        Use Hiragana or Katakana.
                        Do NOT use Romaji.
        
                        Example:
        
                        originalText:
                        私は昨日学校に行きました。
        
                        reading:
                        わたしはきのうがっこうにいきました。
        
                        =========================================================
                        4. VOCABULARY
                        =========================================================
        
                        Identify important vocabulary from the Japanese text.
        
                        Each item contains:
        
                        - word
                        - reading
                        - kanjiReading
                        - meaning
        
                        "word":
                        Preserve Japanese writing and Kanji.
                        Do not replace Kanji with Hiragana.
        
                        "reading":
                        Japanese pronunciation in Hiragana/Katakana.
        
                        "kanjiReading":
                        Sino-Vietnamese / Hán-Việt reading of the Kanji.
        
                        Example:
        
                        word:
                        日本
        
                        reading:
                        にほん
        
                        kanjiReading:
                        NHẬT BẢN
        
                        If there is no Kanji, use "".
        
                        "meaning":
                        Natural Vietnamese meaning according to context.
        
                        =========================================================
                        5. TRANSLATION
                        =========================================================
        
                        "translation" must be a natural Vietnamese translation
                        of the Japanese text.
        
                        If the user originally entered Vietnamese and it was
                        translated into Japanese, translate the resulting Japanese
                        naturally back into Vietnamese while preserving the
                        intended meaning.
        
                        =========================================================
                        6. GRAMMAR
                        =========================================================
        
                        Identify important grammar patterns.
        
                        "pattern":
                        Keep Japanese grammar patterns in Japanese.
        
                        "explanation":
                        Explain in Vietnamese, including meaning and usage.
        
                        Do not unnecessarily convert Kanji to Hiragana.
        
                        If there is no important grammar, return [].
        
                        =========================================================
                        7. SENTENCE STRUCTURE
                        =========================================================
        
                        Explain the Japanese sentence structure in Vietnamese.
        
                        Identify relevant components such as:
                        - Topic / subject
                        - Object
                        - Verb
                        - Adjective
                        - Particles
                        - Time expressions
                        - Modifiers
        
                        Preserve Japanese words and Kanji when showing examples.
        
                        For a single word, explain that it is not a complete sentence.
        
                        =========================================================
                        8. EXAMPLES
                        =========================================================
        
                        Provide useful Japanese example sentences related to
                        the vocabulary or grammar.
        
                        Keep Kanji in example sentences.
                        Do not convert examples to Hiragana only.
        
                        =========================================================
                        9. LANGUAGE RULES
                        =========================================================
        
                        Use Vietnamese for:
                        - translation
                        - vocabulary meaning
                        - grammar explanation
                        - sentence structure explanation
        
                        Use Japanese for:
                        - originalText
                        - reading
                        - vocabulary.word
                        - vocabulary.reading
                        - grammar.pattern
                        - example sentences
        
                        Use Hán-Việt for:
                        - vocabulary.kanjiReading
        
                        =========================================================
                        10. KANJI PRESERVATION
                        =========================================================
        
                        NEVER unnecessarily remove Kanji from:
                        - originalText
                        - vocabulary.word
                        - grammar.pattern
                        - sentence structure
                        - example sentences
        
                        The Japanese text should remain natural and readable
                        for a Japanese learner.
        
                        =========================================================
                        11. OUTPUT
                        =========================================================
        
                        Return ONLY valid JSON according to the provided schema.
        
                        Do not return Markdown.
                        Do not return ```json.
                        Do not add explanations outside the JSON.
        
                        =========================================================
                        USER INPUT
                        =========================================================
        
                        %s
                        """.formatted(text)
                );

        request.put(
                "contents",
                List.of(
                        Map.of(
                                "parts",
                                List.of(part)
                        )
                )
        );

        Map<String, Object> generationConfig =
                new HashMap<>();

        generationConfig.put(
                "responseMimeType",
                "application/json"
        );

        generationConfig.put(
                "responseSchema",
                createGeminiResponseSchema()
        );

        // Giảm thinking để phản hồi nhanh hơn
        generationConfig.put(
                "thinkingConfig",
                Map.of(
                        "thinkingLevel",
                        "minimal"
                )
        );

        request.put(
                "generationConfig",
                generationConfig
        );
        return geminiWebClient
                .post()
                .uri(
                        "/v1beta/models/{model}:generateContent",
                        model
                )
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.createException().map(error -> {
                            logGeminiError(error);
                            if (error.getStatusCode().value() == 429) {
                                return new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                        "AI đang đạt giới hạn sử dụng. Vui lòng thử lại sau.");
                            }
                            return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "Dịch vụ AI hiện chưa khả dụng. Vui lòng thử lại sau.");
                        })
                )
                .bodyToMono(JsonNode.class)
                .map(this::parseResponse)
                .timeout(Duration.ofSeconds(65))
                .onErrorMap(TimeoutException.class, error -> new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT, "AI phản hồi quá lâu. Vui lòng thử lại."))
                .onErrorMap(WebClientRequestException.class, error -> new ResponseStatusException(
                        error.getCause() instanceof io.netty.handler.timeout.ReadTimeoutException
                                ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE,
                        error.getCause() instanceof io.netty.handler.timeout.ReadTimeoutException
                                ? "AI phản hồi quá lâu. Vui lòng thử lại."
                                : "Không kết nối được dịch vụ AI. Vui lòng thử lại sau."));
    }

    // ================= MAPPING =================

    private void logGeminiError(WebClientResponseException error) {
        String status = "UNKNOWN";
        String message = "Phản hồi lỗi không có error.message dạng JSON.";
        try {
            JsonNode details = objectMapper.readTree(error.getResponseBodyAsString());
            if (details != null) {
                status = details.path("error").path("status").asText("UNKNOWN");
                message = details.path("error").path("message").asText(message);
            }
        } catch (JsonProcessingException ignored) {
            // Do not dump HTML or request headers into logs.
        }
        log.warn("Gemini API error: HTTP {}, status={}, message={}",
                error.getStatusCode().value(), sanitizeGeminiError(status), sanitizeGeminiError(message));
    }

    private String sanitizeGeminiError(String value) {
        if (geminiKey != null && !geminiKey.isBlank()) {
            value = value.replace(geminiKey, "[REDACTED]");
        }
        value = value.replaceAll("AIza[\\w-]+", "[REDACTED]")
                .replaceAll("[\\r\\n\\t]", " ");
        return value.substring(0, Math.min(value.length(), 2000));
    }

    private CourseDTO mappingCourseToDTO(Course course) {

        return CourseDTO.builder().courseId(course.getCourseId()).courseName(course.getCourseName()).courseDescription(course.getCourseDescription()).levelId(course.getLevelId()).active(course.getActive().getDescription()).packages(course.getPackages().stream().map(this::mappingPackageToDTO).toList()).build();
    }

    private CoursePackageDTO mappingPackageToDTO(CoursePackage p) {
        return CoursePackageDTO.builder().packageId(p.getPackageId()).packageName(p.getPackageName()).durationDays(p.getDurationDays()).price(p.getPrice()).build();
    }

    private Map<String, Object> createGeminiResponseSchema() {

        Map<String, Object> vocabularySchema =
                Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "word", Map.of(
                                        "type", "STRING"
                                ),
                                "reading", Map.of(
                                        "type", "STRING"
                                ),
                                "meaning", Map.of(
                                        "type", "STRING"
                                ),
                                "kanjiReading", Map.of(
                                        "type", "STRING"
                                )
                        ),
                        "required", List.of(
                                "word",
                                "reading",
                                "kanjiReading",
                                "meaning"
                        )
                );


        Map<String, Object> grammarSchema =
                Map.of(
                        "type", "OBJECT",
                        "properties", Map.of(
                                "pattern", Map.of(
                                        "type", "STRING"
                                ),
                                "explanation", Map.of(
                                        "type", "STRING"
                                )
                        ),
                        "required", List.of(
                                "pattern",
                                "explanation"
                        )
                );


        return Map.of(

                "type", "OBJECT",

                "properties", Map.of(

                        "originalText",
                        Map.of(
                                "type", "STRING"
                        ),

                        "translation",
                        Map.of(
                                "type", "STRING"
                        ),

                        "reading",
                        Map.of(
                                "type", "STRING"
                        ),

                        "vocabulary",
                        Map.of(
                                "type", "ARRAY",
                                "items", vocabularySchema
                        ),

                        "grammar",
                        Map.of(
                                "type", "ARRAY",
                                "items", grammarSchema
                        ),

                        "sentenceStructure",
                        Map.of(
                                "type", "STRING"
                        ),

                        "examples",
                        Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "STRING"
                                )
                        )
                ),

                "required", List.of(
                        "originalText",
                        "translation",
                        "reading",
                        "vocabulary",
                        "grammar",
                        "sentenceStructure",
                        "examples"
                )
        );
    }

    private JapaneseAiResponse parseResponse(JsonNode json) {
        JsonNode candidate = json.path("candidates").path(0);
        String finishReason = candidate.path("finishReason").asText();
        if (!json.path("promptFeedback").path("blockReason").asText().isBlank()
                || List.of("SAFETY", "RECITATION", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII").contains(finishReason)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "AI không thể phân tích nội dung này. Hãy thử từ hoặc câu khác.");
        }
        if (!"STOP".equals(finishReason)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI chưa trả về kết quả đầy đủ. Hãy thử câu ngắn hơn hoặc thử lại.");
        }
        StringBuilder output = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (!part.path("thought").asBoolean(false) && part.path("text").isTextual()) {
                output.append(part.path("text").asText());
            }
        }
        try {
            JsonNode body = objectMapper.readTree(output.toString());
            if (body == null || !body.isObject()) throw new IllegalArgumentException();
            for (String field : List.of("originalText", "translation", "reading", "sentenceStructure")) {
                if (!body.path(field).isTextual() || body.path(field).asText().isBlank()) throw new IllegalArgumentException();
            }
            for (String field : List.of("vocabulary", "grammar", "examples")) {
                if (!body.path(field).isArray()) throw new IllegalArgumentException();
            }
            for (JsonNode item : body.path("vocabulary")) {
                for (String field : List.of("word", "reading", "kanjiReading", "meaning")) {
                    if (!item.path(field).isTextual()) throw new IllegalArgumentException();
                }
            }
            for (JsonNode item : body.path("grammar")) {
                if (!item.path("pattern").isTextual() || !item.path("explanation").isTextual()) throw new IllegalArgumentException();
            }
            for (JsonNode item : body.path("examples")) {
                if (!item.isTextual()) throw new IllegalArgumentException();
            }
            return objectMapper.treeToValue(body, JapaneseAiResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("Gemini response could not be parsed or did not match the analysis schema");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI trả về kết quả không hợp lệ. Vui lòng thử lại.");
        }
    }
}
