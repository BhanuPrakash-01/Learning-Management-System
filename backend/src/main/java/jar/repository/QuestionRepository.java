package jar.repository;

import jar.entity.Question;
import jar.entity.Assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface QuestionRepository
        extends JpaRepository<Question, Long> {

    List<Question> findByAssessment(Assessment assessment);

    long countByAssessment(Assessment assessment);

    @Query("""
            SELECT q FROM Question q
            LEFT JOIN q.assessment a
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(q.questionText) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(q.topic, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(COALESCE(q.subject, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:assessmentId IS NULL OR (a IS NOT NULL AND a.id = :assessmentId))
              AND (:difficulty IS NULL OR :difficulty = '' OR LOWER(COALESCE(q.difficulty, '')) = LOWER(:difficulty))
              AND (:topic IS NULL OR :topic = '' OR LOWER(COALESCE(q.topic, '')) = LOWER(:topic))
            """)
    Page<Question> searchLibrary(@Param("search") String search,
                                 @Param("assessmentId") Long assessmentId,
                                 @Param("difficulty") String difficulty,
                                 @Param("topic") String topic,
                                 Pageable pageable);

    @Query("SELECT q.assessment.id, COUNT(q.id) FROM Question q WHERE q.assessment IS NOT NULL GROUP BY q.assessment.id")
    List<Object[]> countByAssessmentGrouped();
}
