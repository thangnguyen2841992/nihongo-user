package com.thang.nihongo_user;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thang.nihongo_user.model.dto.ExerciseGrade;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ExerciseGradeContractTest {
 @Test void existingGradesWithoutAiSolutionsRemainCompatible()throws Exception {
  var grade=new ObjectMapper().readValue("{\"totalQuestion\":1,\"correctCount\":1,\"wrongCount\":0,\"correctAnswers\":{\"10\":\"C\"}}",ExerciseGrade.class);
  assertEquals("C",grade.correctAnswers().get(10L));assertNull(grade.aiSolutions());
 }
 @Test void forwardingGradePreservesAiExplanationAndEvidence()throws Exception {
  var mapper=new ObjectMapper();var grade=mapper.readValue("""
   {"totalQuestion":1,"correctCount":1,"wrongCount":0,"correctAnswers":{"10":"C"},"aiSolutions":{"10":{"correctAnswer":"C","explanation":"Dấu ★ ở ô thứ 3","evidence":"ので","source":"AI_REASONING"}}}
   """,ExerciseGrade.class);
  var forwarded=mapper.readTree(mapper.writeValueAsString(grade));assertEquals("Dấu ★ ở ô thứ 3",forwarded.path("aiSolutions").path("10").path("explanation").asText());assertEquals("AI_REASONING",forwarded.path("aiSolutions").path("10").path("source").asText());
 }
}
