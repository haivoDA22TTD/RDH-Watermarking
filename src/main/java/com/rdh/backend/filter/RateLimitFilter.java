package com.rdh.backend.filter;

import com.rdh.backend.service.RateLimitService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate Limiting Filter
 * 
 * Filter để apply rate limiting cho tất cả incoming requests.
 * Sử dụng IP address làm key để identify unique users.
 * 
 * Response Headers:
 * - X-RateLimit-Limit: Tổng số requests allowed trong time window
 * - X-RateLimit-Remaining: Số requests còn lại
 * - X-RateLimit-Reset: Thời gian (giây) để refill tokens
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-09-22
 */
@Component
@Order(1) // Execute sớm nhất trong filter chain
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitService rateLimitService;

    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // Get client identifier (IP address)
        String clientId = getClientId(request);
        
        // Get bucket for this client
        Bucket bucket = rateLimitService.resolveBucket(clientId);
        
        // Try to consume 1 token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Request allowed
            
            // Add rate limit headers to response
            response.addHeader("X-RateLimit-Limit", "100");
            response.addHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            
            log.debug("Rate limit check passed for client: {} (remaining: {})",
                    clientId, probe.getRemainingTokens());
            
            // Continue with the request
            filterChain.doFilter(request, response);
            
        } else {
            // Rate limit exceeded
            
            // Calculate wait time until next token available
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000; // Convert to seconds
            
            // Add rate limit headers
            response.addHeader("X-RateLimit-Limit", "100");
            response.addHeader("X-RateLimit-Remaining", "0");
            response.addHeader("X-RateLimit-Reset", String.valueOf(waitForRefill));
            response.addHeader("Retry-After", String.valueOf(waitForRefill));
            
            // Set 429 Too Many Requests status
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            // Write error response
            String errorJson = String.format(
                    "{\"code\":%d,\"message\":\"Too many requests. Please try again in %d seconds.\",\"data\":null}",
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    waitForRefill
            );
            
            response.getWriter().write(errorJson);
            
            log.warn("Rate limit exceeded for client: {} (wait: {} seconds)",
                    clientId, waitForRefill);
        }
    }

    /**
     * Get client identifier từ request.
     * 
     * Priority:
     * 1. X-Forwarded-For header (nếu có proxy/load balancer)
     * 2. X-Real-IP header (nếu có nginx)
     * 3. Remote address
     * 
     * @param request HTTP request
     * @return Client identifier (IP address)
     */
    private String getClientId(HttpServletRequest request) {
        // Check X-Forwarded-For header (proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For có thể chứa nhiều IPs: "client, proxy1, proxy2"
            // Lấy IP đầu tiên (client IP)
            return xForwardedFor.split(",")[0].trim();
        }
        
        // Check X-Real-IP header (nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Các paths không cần rate limiting.
     * 
     * @param request HTTP request
     * @return true nếu không cần filter, false nếu cần filter
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip rate limiting cho static resources
        return path.startsWith("/static/") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/") ||
               path.endsWith(".html") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".ico");
    }
}
