package jar.config;

import jar.entity.Assessment;
import jar.entity.PracticeCategory;
import jar.entity.PracticeQuestion;
import jar.entity.PracticeTopic;
import jar.entity.Question;
import jar.entity.Role;
import jar.entity.User;
import jar.repository.AssessmentRepository;
import jar.repository.PracticeCategoryRepository;
import jar.repository.PracticeQuestionRepository;
import jar.repository.PracticeTopicRepository;
import jar.repository.QuestionRepository;
import jar.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Configuration
public class DataInitializer {

    private static final int TARGET_STUDENTS = 60;
    private static final int TARGET_ASSESSMENTS = 10;
    private static final int TARGET_PRACTICE_TOPICS = 10;

    @Bean
    CommandLineRunner seedDemoData(PracticeCategoryRepository categoryRepo,
                                   PracticeTopicRepository topicRepo,
                                   PracticeQuestionRepository practiceQuestionRepo,
                                   UserRepository userRepo,
                                   AssessmentRepository assessmentRepo,
                                   QuestionRepository questionRepo,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            seedPracticeCategories(categoryRepo);
            seedStudents(userRepo, passwordEncoder);
            seedAssessments(assessmentRepo, questionRepo);
            seedPracticeTopicsAndQuestions(categoryRepo, topicRepo, practiceQuestionRepo);
        };
    }

    private void seedPracticeCategories(PracticeCategoryRepository categoryRepo) {
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
    }

    private void seedStudents(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        long currentStudents = userRepo.countByRole(Role.STUDENT);
        if (currentStudents >= 50) {
            return;
        }

        List<String> branches = List.of("CSE", "ECE", "EEE", "IT", "MECH");
        List<String> sections = List.of("A", "B", "C", "D", "E");
        Map<String, String> streamCodes = Map.of(
                "CSE", "06",
                "ECE", "04",
                "EEE", "03",
                "IT", "12",
                "MECH", "01"
        );

        int start = (int) currentStudents;
        for (int i = start + 1; i <= TARGET_STUDENTS; i++) {
            String branch = branches.get((i - 1) % branches.size());
            String section = sections.get((i - 1) % sections.size());
            int batchYear = 2024 + ((i - 1) % 3);
            String streamCode = streamCodes.getOrDefault(branch, "06");
            String rollNumber = String.format("%02dEG1%s%s%02d", batchYear % 100, streamCode, section, i % 100);
            String email = String.format("student%03d@anurag.ac.in", i);

            if (userRepo.findByEmail(email).isPresent() || userRepo.findByRollNumber(rollNumber).isPresent()) {
                continue;
            }

            User student = User.builder()
                    .name("Student " + i)
                    .email(email)
                    .password(passwordEncoder.encode("Password@123"))
                    .rollNumber(rollNumber)
                    .branch(branch)
                    .batchYear(batchYear)
                    .section(section)
                    .phone(String.format("9%09d", i))
                    .active(true)
                    .emailVerified(true)
                    .forcePasswordChange(false)
                    .role(Role.STUDENT)
                    .build();
            userRepo.save(student);
        }
    }

    private void seedAssessments(AssessmentRepository assessmentRepo,
                                 QuestionRepository questionRepo) {
        long currentAssessments = assessmentRepo.count();
        if (currentAssessments >= TARGET_ASSESSMENTS) {
            return;
        }

        List<String> assessmentTypes = List.of("WEEKLY_TEST", "MOCK_TEST", "BATCH_ASSESSMENT");
        List<String> branches = List.of("CSE", "ECE", "EEE", "IT", "MECH");
        LocalDateTime now = LocalDateTime.now();

        for (int i = (int) currentAssessments + 1; i <= TARGET_ASSESSMENTS; i++) {
            Assessment assessment = Assessment.builder()
                    .title("Demo Assessment " + i)
                    .description("Auto-seeded assessment for QA and frontend testing.")
                    .duration(30 + ((i - 1) % 4) * 15)
                    .assessmentType(assessmentTypes.get((i - 1) % assessmentTypes.size()))
                    .targetBranches(branches.get((i - 1) % branches.size()))
                    .targetBatches("2024,2025,2026")
                    .targetSections("A,B,C")
                    .startTime(now.minusDays(i))
                    .endTime(now.plusDays(20 + i))
                    .allowLateSubmission(true)
                    .maxAttempts(2)
                    .negativeMarking(i % 2 == 0)
                    .penaltyFraction(new BigDecimal("0.25"))
                    .reviewAfterClose(true)
                    .active(true)
                    .build();

            Assessment savedAssessment = assessmentRepo.save(assessment);

            for (int q = 1; q <= 10; q++) {
                int left = i + q;
                int right = q;
                int correct = left + right;

                Question question = Question.builder()
                        .assessment(savedAssessment)
                        .questionText("Assessment " + i + " Q" + q + ": What is " + left + " + " + right + "?")
                        .optionA(String.valueOf(correct - 1))
                        .optionB(String.valueOf(correct))
                        .optionC(String.valueOf(correct + 1))
                        .optionD(String.valueOf(correct + 2))
                        .correctAnswer("B")
                        .subject("Aptitude")
                        .topic("Arithmetic")
                        .difficulty(q <= 3 ? "Easy" : (q <= 7 ? "Medium" : "Hard"))
                        .build();
                questionRepo.save(question);
            }
        }
    }

    private void seedPracticeTopicsAndQuestions(PracticeCategoryRepository categoryRepo,
                                                PracticeTopicRepository topicRepo,
                                                PracticeQuestionRepository practiceQuestionRepo) {
        List<PracticeCategory> categories = categoryRepo.findAll();
        if (categories.isEmpty()) {
            return;
        }

        long currentTopics = topicRepo.count();
        for (int i = (int) currentTopics + 1; i <= TARGET_PRACTICE_TOPICS; i++) {
            PracticeCategory category = categories.get((i - 1) % categories.size());
            PracticeTopic topic = PracticeTopic.builder()
                    .category(category)
                    .name("Practice Topic " + i)
                    .description("Auto-seeded practice topic " + i)
                    .icon("book")
                    .active(true)
                    .build();
            topicRepo.save(topic);
        }

        List<PracticeTopic> topics = topicRepo.findAll();
        for (int i = 0; i < Math.min(TARGET_PRACTICE_TOPICS, topics.size()); i++) {
            PracticeTopic topic = topics.get(i);
            long currentQuestions = practiceQuestionRepo.countByTopic(topic);
            if (currentQuestions >= 10) {
                continue;
            }

            for (int q = (int) currentQuestions + 1; q <= 10; q++) {
                int left = (i + 2) * q;
                int right = q + 1;
                int correct = left + right;

                String optionsJson = String.format("[\"%d\",\"%d\",\"%d\",\"%d\"]",
                        correct - 1, correct, correct + 1, correct + 2);

                PracticeQuestion question = PracticeQuestion.builder()
                        .topic(topic)
                        .questionText("Practice " + topic.getName() + " Q" + q + ": What is " + left + " + " + right + "?")
                        .options(optionsJson)
                        .correctAnswer(String.valueOf(correct))
                        .explanation("Add the two numbers directly.")
                        .difficulty(q <= 3 ? "Easy" : (q <= 7 ? "Medium" : "Hard"))
                        .type("MCQ")
                        .build();
                practiceQuestionRepo.save(question);
            }
        }
    }
}
