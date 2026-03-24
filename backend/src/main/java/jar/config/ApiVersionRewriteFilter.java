package jar.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiVersionRewriteFilter extends OncePerRequestFilter {

    private static final String VERSION_PREFIX = "/api/v1";
    private static final String BASE_PREFIX = "/api";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = pathWithinApplication(request);
        return !(VERSION_PREFIX.equals(path) || path.startsWith(VERSION_PREFIX + "/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String path = pathWithinApplication(request);
        String suffix = path.substring(VERSION_PREFIX.length());
        String rewrittenPath = BASE_PREFIX + suffix;
        if (rewrittenPath.isBlank()) {
            rewrittenPath = BASE_PREFIX;
        }

        String finalRewrittenPath = rewrittenPath;
        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(request) {
            @Override
            public String getRequestURI() {
                return contextPath + finalRewrittenPath;
            }

            @Override
            public String getServletPath() {
                return finalRewrittenPath;
            }

            @Override
            public StringBuffer getRequestURL() {
                String scheme = getScheme();
                int serverPort = getServerPort();
                String host = getServerName();
                boolean defaultPort = ("http".equalsIgnoreCase(scheme) && serverPort == 80)
                        || ("https".equalsIgnoreCase(scheme) && serverPort == 443);
                String authority = defaultPort ? host : host + ":" + serverPort;
                return new StringBuffer(scheme + "://" + authority + getRequestURI());
            }
        };

        filterChain.doFilter(wrapper, response);
    }

    private String pathWithinApplication(HttpServletRequest request) {
        String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
        String requestUri = request.getRequestURI() == null ? "" : request.getRequestURI();
        if (!contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }
        return requestUri;
    }
}
