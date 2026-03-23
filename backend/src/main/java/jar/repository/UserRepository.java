package jar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jar.entity.User;

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
}
