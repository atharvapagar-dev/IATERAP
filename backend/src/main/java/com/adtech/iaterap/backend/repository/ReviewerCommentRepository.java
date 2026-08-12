package com.adtech.iaterap.backend.repository;

import com.adtech.iaterap.backend.entity.ReviewerComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewerCommentRepository extends JpaRepository<ReviewerComment, Long> {

    List<ReviewerComment> findByProcedureHistoryIdOrderByCreatedAtAsc(Long procedureHistoryId);
}
