package jar.service.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;

@Service
public class InputSanitizerService {

    private static final PolicyFactory PLAIN_TEXT_POLICY = new HtmlPolicyBuilder().toFactory();

    private static final PolicyFactory RICH_TEXT_POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS);

    public String sanitizePlainText(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = PLAIN_TEXT_POLICY.sanitize(value).trim();
        return sanitized.isBlank() ? null : sanitized;
    }

    public String sanitizeRichText(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = RICH_TEXT_POLICY.sanitize(value).trim();
        return sanitized.isBlank() ? null : sanitized;
    }
}
