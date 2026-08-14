package com.thang.nihongo_user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.model.dto.*;
import com.thang.nihongo_user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final ICourseRepository courseRepository;
    private final ICoursePackageRepository coursePackageRepository;
    private final IUserSubscriptionRepository subscriptionRepository;
    private final IStaffClient staffClient;
    private final IUserClient userClient;
    private final IUserExerciseAttemptRepository userExerciseAttemptRepository;
    private final WebClient openAiWebClient;
    private final ObjectMapper objectMapper;
    @Value("${openai.model}")
    private String model;
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

    // ================= SUBSCRIPTION =================
    @Override
    @Transactional
    public UserSubscription createSubscription(Long userId, Long courseId, Long packageId) {

        if (subscriptionRepository.existsByUserIdAndCourseIdAndPackageId(userId, courseId, packageId)) {
            throw new RuntimeException("Already subscribed");
        }

        CoursePackage pack = coursePackageRepository.findById(packageId).orElseThrow(() -> new RuntimeException("Package not found"));

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(pack.getDurationDays());

        UserSubscription sub = UserSubscription.builder().userId(userId).courseId(courseId).packageId(packageId).progress(0).createdAt(start).expiredAt(end).build();

        return subscriptionRepository.save(sub);
    }

    @Override
    public UserSubscription renewSubscription(Long userId, Long courseId, Long packageId) {
        UserSubscription subscription = subscriptionRepository.findByUserIdAndCourseId(userId, courseId).orElseThrow(() -> new RuntimeException("Subscription not found"));

        CoursePackage pack = coursePackageRepository.findById(packageId).orElseThrow(() -> new RuntimeException("Package not found"));

        LocalDateTime now = LocalDateTime.now();

        // Nếu còn hạn thì cộng thêm
        LocalDateTime start = subscription.getExpiredAt().isAfter(now) ? subscription.getExpiredAt() : now;

        subscription.setPackageId(packageId);
        subscription.setExpiredAt(start.plusDays(pack.getDurationDays()));

        return subscriptionRepository.save(subscription);
    }


    @Override
    public List<Long> findCourseIdsByUserId(Long userId) {
        return subscriptionRepository.findByUserId(userId).stream().map(UserSubscription::getCourseId).distinct().toList();
    }

    @Override
    public boolean hasActiveSubscription(Long userId, Long courseId) {
        return subscriptionRepository.existsActive(userId, courseId);
    }

    @Override
    public List<MyCourseDTO> findMyCourses(Long userId) {

        return subscriptionRepository.findByUserId(userId).stream().map(sub -> {

            Course course = courseRepository.findById(sub.getCourseId()).orElseThrow(() -> new RuntimeException("Course not found"));

            CoursePackage pack = coursePackageRepository.findById(sub.getPackageId()).orElseThrow(() -> new RuntimeException("Package not found"));

            return MyCourseDTO.builder().courseId(course.getCourseId()).courseName(course.getCourseName()).packageName(pack.getPackageName()).progress(sub.getProgress()).enrolledAt(sub.getCreatedAt())   // ✅ FIX
                    .expiredAt(sub.getExpiredAt())    // ✅ FIX
                    .build();
        }).toList();
    }

    @Override
    public void submitExerciseAttempt(String userEmail, SubmitLessonResultRequest request) {
        UserDTO user = this.userClient.findUserByEmail(userEmail);

        LessonResponse lesson = this.staffClient.getLessonById(request.getLessonId());


        double score = request.getCorrectCount() * 100.0 / request.getTotalQuestion();

        UserExerciseAttempt result = UserExerciseAttempt.builder().userId(user.getId()).lessonId(lesson.getLessonId()).totalQuestion(request.getTotalQuestion()).correctCount(request.getCorrectCount()).wrongCount(request.getWrongCount()).score(score).submittedAt(LocalDateTime.now()).build();

        this.userExerciseAttemptRepository.save(result);
    }

    @Override
    public List<LessonResultResponse> getMyResults(String userEmail) {
        UserDTO user = this.userClient.findUserByEmail(userEmail);
        return this.userExerciseAttemptRepository.findByUserIdOrderBySubmittedAtDesc(user.getId()).stream().map(this::convert).toList();
    }

    @Override
    public List<LessonResultResponse> getLessonResults(String userEmail, Long lessonId) {
        UserDTO user = this.userClient.findUserByEmail(userEmail);
        return this.userExerciseAttemptRepository.findByUserIdAndLessonIdOrderBySubmittedAtDesc(user.getId(), lessonId).stream().map(this::convert).toList();
    }

    @Override
    public LessonResultResponse convert(UserExerciseAttempt entity) {
        return LessonResultResponse.builder().resultId(entity.getUserExerciseAttemptId()).lessonId(this.staffClient.getLessonById(entity.getLessonId()).getLessonId()).lessonName(this.staffClient.getLessonById(entity.getLessonId()).getName()).totalQuestion(entity.getTotalQuestion()).correctCount(entity.getCorrectCount()).wrongCount(entity.getWrongCount()).score(entity.getScore()).submittedAt(entity.getSubmittedAt()).build();
    }

    @Override
    public Mono<JapaneseAiResponse> analyzeJapanese(String text) {
        Map<String, Object> request = new HashMap<>();

        request.put("model", model);

        request.put(
                "instructions",
                """
                You are an expert Japanese teacher
                helping Vietnamese students.

                Analyze the Japanese sentence provided
                by the user.

                Return the answer strictly according
                to the provided JSON schema.

                All explanations must be written in Vietnamese.

                Keep the Japanese words and grammar patterns
                in Japanese.
                """
        );

        request.put(
                "input",
                text
        );

        request.put(
                "text",
                createStructuredOutputSchema()
        );

        return openAiWebClient
                .post()
                .uri("/v1/responses")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseResponse);
    }

    // ================= MAPPING =================

    private CourseDTO mappingCourseToDTO(Course course) {

        return CourseDTO.builder().courseId(course.getCourseId()).courseName(course.getCourseName()).courseDescription(course.getCourseDescription()).levelId(course.getLevelId()).active(course.getActive().getDescription()).packages(course.getPackages().stream().map(this::mappingPackageToDTO).toList()).build();
    }

    private CoursePackageDTO mappingPackageToDTO(CoursePackage p) {
        return CoursePackageDTO.builder().packageId(p.getPackageId()).packageName(p.getPackageName()).durationDays(p.getDurationDays()).price(p.getPrice()).build();
    }
    private Map<String, Object> createStructuredOutputSchema() {

        Map<String, Object> vocabularyItem =
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "word", Map.of(
                                        "type", "string"
                                ),
                                "reading", Map.of(
                                        "type", "string"
                                ),
                                "meaning", Map.of(
                                        "type", "string"
                                )
                        ),
                        "required", List.of(
                                "word",
                                "reading",
                                "meaning"
                        ),
                        "additionalProperties", false
                );

        Map<String, Object> grammarItem =
                Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "pattern", Map.of(
                                        "type", "string"
                                ),
                                "explanation", Map.of(
                                        "type", "string"
                                )
                        ),
                        "required", List.of(
                                "pattern",
                                "explanation"
                        ),
                        "additionalProperties", false
                );

        Map<String, Object> schema =
                Map.of(
                        "type", "object",

                        "properties", Map.of(

                                "translation",
                                Map.of(
                                        "type", "string"
                                ),

                                "reading",
                                Map.of(
                                        "type", "string"
                                ),

                                "vocabulary",
                                Map.of(
                                        "type", "array",
                                        "items", vocabularyItem
                                ),

                                "grammar",
                                Map.of(
                                        "type", "array",
                                        "items", grammarItem
                                ),

                                "sentenceStructure",
                                Map.of(
                                        "type", "string"
                                ),

                                "examples",
                                Map.of(
                                        "type", "array",
                                        "items", Map.of(
                                                "type", "string"
                                        )
                                )
                        ),

                        "required", List.of(
                                "translation",
                                "reading",
                                "vocabulary",
                                "grammar",
                                "sentenceStructure",
                                "examples"
                        ),

                        "additionalProperties", false
                );

        return Map.of(
                "format",
                Map.of(
                        "type", "json_schema",
                        "name", "japanese_analysis",
                        "strict", true,
                        "schema", schema
                )
        );
    }
    private JapaneseAiResponse parseResponse(JsonNode json) {

        String outputText =
                json.path("output_text")
                        .asText();

        try {

            ObjectMapper mapper =
                    new ObjectMapper();

            return mapper.readValue(
                    outputText,
                    JapaneseAiResponse.class
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Cannot parse OpenAI response",
                    e
            );
        }
    }
}