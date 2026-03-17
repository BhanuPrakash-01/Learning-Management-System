package jar.service.impl;

import jar.entity.Attempt;
import jar.repository.AttemptRepository;
import jar.service.AttemptService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AttemptScheduler {

    private final AttemptRepository attemptRepo;
    private final AttemptService attemptService;

    public AttemptScheduler(AttemptRepository attemptRepo,
                             AttemptService attemptService) {
        this.attemptRepo = attemptRepo;
        this.attemptService = attemptService;
    }

    @Scheduled(fixedRate = 60000) // every 1 minute
    public void autoSubmitExpiredAttempts() {

        List<Attempt> attempts =
                attemptRepo.findAll();

        for (Attempt a : attempts) {

            if (!a.isSubmitted()
                    && LocalDateTime.now()
                    .isAfter(a.getDeadline())) {

                attemptService.submitAttempt(a.getId());
            }
        }
    }
}