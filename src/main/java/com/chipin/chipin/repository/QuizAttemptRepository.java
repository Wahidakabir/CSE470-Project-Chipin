package com.chipin.chipin.repository;

import com.chipin.chipin.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    Optional<QuizAttempt> findByQuestionIdAndUserId(Long questionId, String userId);
    List<QuizAttempt> findByUserId(String userId);
}