package com.fersal.trivia.question;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    @EntityGraph(attributePaths = {"options", "category"})
    List<Question> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = {"options", "category"})
    Optional<Question> findWithOptionsById(Long id);

    @EntityGraph(attributePaths = {"options", "category"})
    @Query("""
        SELECT q FROM Question q
        WHERE (:categoryId IS NULL OR q.category.id = :categoryId)
          AND (:active IS NULL OR q.active = :active)
        ORDER BY q.id DESC
    """)
    Page<Question> findFiltered(@Param("categoryId") Long categoryId,
                                @Param("active") Boolean active,
                                Pageable pageable);

    long countByCategoryIdAndActiveTrue(Long categoryId);

    @Query(value = "SELECT selected_question_id FROM assign_and_lock_questions_for_tablet(:gameId, :tabletCode, :categoryId)",
            nativeQuery = true)
    List<Long> assignAndLockQuestionsForTablet(@Param("gameId") UUID gameId,
                                               @Param("tabletCode") String tabletCode,
                                               @Param("categoryId") Long categoryId);
}