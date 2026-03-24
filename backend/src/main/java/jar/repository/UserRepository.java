package jar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jar.entity.User;
import jar.entity.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByRollNumber(String rollNumber);

    @Query("""
            SELECT u FROM User u
            WHERE (:search IS NULL OR :search = '' OR
                   LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.rollNumber) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:branch IS NULL OR :branch = '' OR u.branch = :branch)
              AND (:batchYear IS NULL OR u.batchYear = :batchYear)
              AND (:section IS NULL OR :section = '' OR u.section = :section)
            """)
    List<User> searchStudents(@Param("search") String search,
                              @Param("branch") String branch,
                              @Param("batchYear") Integer batchYear,
                              @Param("section") String section);

    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND (:search IS NULL OR :search = '' OR
                   LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.rollNumber) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:branch IS NULL OR :branch = '' OR u.branch = :branch)
              AND (:batchYear IS NULL OR u.batchYear = :batchYear)
              AND (:section IS NULL OR :section = '' OR u.section = :section)
            """)
    Page<User> searchUsersByRole(@Param("role") Role role,
                                 @Param("search") String search,
                                 @Param("branch") String branch,
                                 @Param("batchYear") Integer batchYear,
                                 @Param("section") String section,
                                 Pageable pageable);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    List<User> findByRole(Role role);

    @Query("""
            SELECT COUNT(u) FROM User u
            WHERE u.role = :role
              AND NOT EXISTS (
                SELECT 1 FROM Attempt a WHERE a.student = u
              )
            """)
    long countByRoleWithNoAttempts(@Param("role") Role role);

    @Query("""
            SELECT COALESCE(u.branch, 'UNKNOWN'), COUNT(u)
            FROM User u
            WHERE u.role = :role
            GROUP BY u.branch
            """)
    List<Object[]> branchBreakdownByRole(@Param("role") Role role);

    @Query("""
            SELECT COALESCE(u.batchYear, 0), COUNT(u)
            FROM User u
            WHERE u.role = :role
            GROUP BY u.batchYear
            """)
    List<Object[]> batchBreakdownByRole(@Param("role") Role role);

    @Query("""
            SELECT u FROM User u
            WHERE u.role = :role
              AND NOT EXISTS (
                SELECT 1 FROM Attempt a
                WHERE a.student = u
                  AND a.startTime IS NOT NULL
                  AND a.startTime > :after
              )
            """)
    Page<User> findLowPerformers(@Param("role") Role role,
                                 @Param("after") java.time.LocalDateTime after,
                                 Pageable pageable);
}
