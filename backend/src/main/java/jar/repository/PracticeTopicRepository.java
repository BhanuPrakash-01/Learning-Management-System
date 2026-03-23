package jar.repository;

import jar.entity.PracticeCategory;
import jar.entity.PracticeTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PracticeTopicRepository extends JpaRepository<PracticeTopic, Long> {
    List<PracticeTopic> findByCategory(PracticeCategory category);
}
