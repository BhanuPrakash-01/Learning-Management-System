package jar.service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchService.class);

    private final JavaMailSender mailSender;
    private final String from;

    public EmailDispatchService(JavaMailSender mailSender,
                                @Value("${app.mail.from:no-reply@anurag.ac.in}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public boolean sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.warn("Email delivery failed for {} ({}). Falling back to server logs.", to, subject, ex);
            log.info("Email fallback -> to: {}, subject: {}, body: {}", to, subject, text);
            return false;
        }
    }
}
