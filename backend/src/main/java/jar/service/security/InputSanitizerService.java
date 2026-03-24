package jar.service.security;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.regex.Pattern;

@Service
public class InputSanitizerService {

    private static final PolicyFactory PLAIN_TEXT_POLICY = new HtmlPolicyBuilder().toFactory();

    private static final PolicyFactory RICH_TEXT_POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS);

    // Strip HTML tags while preserving plain-text symbols like <, >, &, /, +, -, =
    private static final Pattern HTML_TAG_PATTERN =
            Pattern.compile("(?is)<\\s*/?\\s*[a-z][a-z0-9:-]*(\\s+[^<>]*)?>");

    public String sanitizePlainText(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = PLAIN_TEXT_POLICY.sanitize(value).trim();
        String unescaped = HtmlUtils.htmlUnescape(sanitized).trim();
        return unescaped.isBlank() ? null : unescaped;
    }

    public String sanitizePlainTextPermissive(String value) {
        if (value == null) {
            return null;
        }
        String withoutTags = HTML_TAG_PATTERN.matcher(value).replaceAll(" ");
        String sanitized = PLAIN_TEXT_POLICY.sanitize(withoutTags).trim();
        String unescaped = HtmlUtils.htmlUnescape(sanitized).trim();
        return unescaped.isBlank() ? null : unescaped;
    }

    public String sanitizeRichText(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = RICH_TEXT_POLICY.sanitize(value).trim();
        return sanitized.isBlank() ? null : sanitized;
    }
}
