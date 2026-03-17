package jar.service.impl;

import jar.dto.QuestionRequest;
import jar.entity.Assessment;
import jar.entity.Question;
import jar.repository.AssessmentRepository;
import jar.repository.QuestionRepository;
import jar.service.QuestionService;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepo;
    private final AssessmentRepository assessmentRepo;

    public QuestionServiceImpl(QuestionRepository questionRepo,
                               AssessmentRepository assessmentRepo) {
        this.questionRepo = questionRepo;
        this.assessmentRepo = assessmentRepo;
    }

    @Override
    public Question addQuestion(QuestionRequest request) {

        Assessment assessment = assessmentRepo.findById(request.getAssessmentId())
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        Question question = Question.builder()
                .questionText(request.getQuestionText())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .correctAnswer(request.getCorrectAnswer())
                .assessment(assessment)
                .build();

        return questionRepo.save(question);
    }

    @Override
    public List<Question> getAll() {
        return questionRepo.findAll();
    }

    @Override
    public List<Question> getByAssessment(Long assessmentId) {

        Assessment assessment = assessmentRepo.findById(assessmentId)
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        return questionRepo.findByAssessment(assessment);
    }

    @Override
    public Question updateQuestion(Long id, QuestionRequest request) {

        Question question = questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Assessment assessment = assessmentRepo.findById(request.getAssessmentId())
                .orElseThrow(() -> new RuntimeException("Assessment not found"));

        question.setQuestionText(request.getQuestionText());
        question.setOptionA(request.getOptionA());
        question.setOptionB(request.getOptionB());
        question.setOptionC(request.getOptionC());
        question.setOptionD(request.getOptionD());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setAssessment(assessment);

        return questionRepo.save(question);
    }

    @Override
    public void deleteQuestion(Long id) {
        questionRepo.deleteById(id);
    }
}