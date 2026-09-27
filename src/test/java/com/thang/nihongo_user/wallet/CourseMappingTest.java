package com.thang.nihongo_user.wallet;
import com.thang.nihongo_user.model.*;
import com.thang.nihongo_user.model.dto.CreateCourseRequest;
import com.thang.nihongo_user.repository.*;
import com.thang.nihongo_user.service.UserServiceImpl;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CourseMappingTest {
 ICourseRepository courses=mock(ICourseRepository.class);
 IStaffClient staff=mock(IStaffClient.class);
 UserServiceImpl service=new UserServiceImpl(courses,mock(ICoursePackageRepository.class),mock(IUserSubscriptionRepository.class),staff,mock(IUserClient.class),mock(IUserExerciseAttemptRepository.class),mock(org.springframework.web.reactive.function.client.WebClient.class),new com.fasterxml.jackson.databind.ObjectMapper());
 @Test void creatingACourseWithoutPackagesReturnsAnEmptyPackageList(){
  when(courses.save(any())).thenAnswer(i->i.getArgument(0));
  var result=service.createNewCourse(new CreateCourseRequest("N5","Intro",1L,CourseStatus.ACTIVE).toEntity());
  assertNotNull(result.getPackages());assertTrue(result.getPackages().isEmpty());
 }
 @Test void expiredContentAccessDoesNotHideOwnedResults(){
  var request=feign.Request.create(feign.Request.HttpMethod.GET,"http://staff/lessons/1",Map.of(),null,StandardCharsets.UTF_8,null);
  when(staff.getLessonById(1L)).thenThrow(new feign.FeignException.Forbidden("expired",request,null,Map.of()));
  var result=service.convert(UserExerciseAttempt.builder().lessonId(1L).userId("owner").totalQuestion(2).correctCount(1).wrongCount(1).score(50.0).build());
  assertEquals("Bài học 1",result.getLessonName());assertEquals(50.0,result.getScore());
 }
}
