package com.thang.nihongo_user.model.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Map;
@Getter @Setter
public class SubmitLessonResultRequest {
 @NotNull @Positive private Long lessonId;
 @Pattern(regexp="[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") private String submissionId;
 @NotNull @Size(max=2000) private Map<@NotNull @Positive Long,@NotNull @Pattern(regexp="[ABCD]") String> answers;
}
