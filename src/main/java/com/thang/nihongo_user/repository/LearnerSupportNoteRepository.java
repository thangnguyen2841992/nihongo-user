package com.thang.nihongo_user.repository;
import com.thang.nihongo_user.model.LearnerSupportNote;import org.springframework.data.jpa.repository.JpaRepository;import org.springframework.data.domain.*;
public interface LearnerSupportNoteRepository extends JpaRepository<LearnerSupportNote,Long>{Page<LearnerSupportNote> findByUserIdOrderByCreatedAtDescIdDesc(String userId,Pageable page);}
