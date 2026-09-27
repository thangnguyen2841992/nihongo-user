package com.thang.nihongo_user.model.dto;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Map;
@Getter @Setter
public class SubmitLessonResultRequest {
 @NotNull @Positive private Long lessonId;
 @NotNull @Size(max=2000) private Map<@NotNull @Positive Long,@NotNull @Pattern(regexp="[ABCD]") String> answers;
}
