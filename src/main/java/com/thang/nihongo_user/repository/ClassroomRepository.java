package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.Classroom;import org.springframework.data.jpa.repository.*;import org.springframework.data.domain.*;import jakarta.persistence.LockModeType;import java.util.*;
public interface ClassroomRepository extends JpaRepository<Classroom,Long> {
 Page<Classroom> findByOwnerOrderByCreatedAtDescIdDesc(String owner,Pageable page);
 Page<Classroom> findAllByOrderByCreatedAtDescIdDesc(Pageable page);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from Classroom c where c.id=:id") Optional<Classroom> lock(Long id);
}
