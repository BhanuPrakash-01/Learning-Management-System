package jar.config;

import jar.service.auth.AdminBootstrapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminBootstrapInitializer {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);

    @Bean
    CommandLineRunner bootstrapAdminRunner(AdminBootstrapService adminBootstrapService) {
        return args -> {
            var admin = adminBootstrapService.bootstrapFromEnvironmentIfNeeded();
            if (admin != null) {
                log.info("Admin bootstrap completed for {}", admin.getEmail());
            }
        };
    }
}
