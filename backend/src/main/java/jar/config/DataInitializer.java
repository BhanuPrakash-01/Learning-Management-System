package jar.config;

import jar.entity.PracticeCategory;
import jar.repository.PracticeCategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedPracticeCategories(PracticeCategoryRepository categoryRepo) {
        return args -> {
            if (categoryRepo.count() > 0) {
                return;
            }
            List<String> categories = List.of(
                    "Quantitative Aptitude",
                    "Logical Reasoning",
                    "Verbal Ability",
                    "Coding",
                    "Technical"
            );
            categories.forEach(name -> categoryRepo.save(PracticeCategory.builder().name(name).build()));
        };
    }
}
