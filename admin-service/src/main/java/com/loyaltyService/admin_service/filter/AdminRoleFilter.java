package com.loyaltyService.admin_service.filter;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@Order(1)
public class AdminRoleFilter implements Filter {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/actuator", "/swagger", "/v3/api-docs", "/health"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        // Allow public paths
        if (PUBLIC_PATHS.stream().anyMatch(path::contains)) {
            chain.doFilter(req, res);
            return;
        }

        String role   = request.getHeader("X-User-Role");
        String userId = request.getHeader("X-User-Id");

        // No identity headers means request didn't come through the gateway
        if (userId == null || userId.isBlank()) {
            log.warn("Admin request rejected — no X-User-Id header on path: {}", path);
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Unauthorized — request must go through the API gateway");
            return;
        }

        // Role check
        if (!"ADMIN".equals(role) && !"SUPPORT".equals(role)) {
            log.warn("Admin request rejected — userId={} has role={} on path: {}", userId, role, path);
            writeError(response, HttpServletResponse.SC_FORBIDDEN,
                    "Access denied — ADMIN role required. Your role: " + role);
            return;
        }

        log.debug("Admin access granted: userId={}, role={}, path={}", userId, role, path);
        chain.doFilter(req, res);
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(
                String.format("{\"success\":false,\"status\":%d,\"message\":\"%s\"}", status, message));
    }
}
