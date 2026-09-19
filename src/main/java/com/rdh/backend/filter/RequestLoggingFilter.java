package com.rdh.backend.filter;

import com.rdh.backend.util.LoggingUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter to log incoming requests and outgoing responses
 * Also sets up MDC (Mapped Diagnostic Context) for request tracking
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter implements Filter {
    
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            // Wrap request/response for content caching
            ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(httpRequest);
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(httpResponse);
            
            long startTime = System.currentTimeMillis();
            
            try {
                // Setup MDC
                setupMDC(wrappedRequest);
                
                // Log incoming request
                logRequest(wrappedRequest);
                
                // Continue filter chain
                chain.doFilter(wrappedRequest, wrappedResponse);
                
                // Calculate duration
                long duration = System.currentTimeMillis() - startTime;
                
                // Log response
                logResponse(wrappedRequest, wrappedResponse, duration);
                
                // Copy response body back
                wrappedResponse.copyBodyToResponse();
                
            } finally {
                // Clear MDC after request completion
                LoggingUtils.clearMDC();
            }
        } else {
            chain.doFilter(request, response);
        }
    }
    
    /**
     * Setup MDC with request ID, user ID, session ID
     */
    private void setupMDC(HttpServletRequest request) {
        // Get or generate request ID
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isEmpty()) {
            requestId = LoggingUtils.generateRequestId();
        }
        LoggingUtils.setRequestId(requestId);
        
        // Get user ID from header (if present)
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.isEmpty()) {
            LoggingUtils.setUserId(userId);
        }
        
        // Get session ID
        if (request.getSession(false) != null) {
            LoggingUtils.setSessionId(request.getSession().getId());
        }
    }
    
    /**
     * Log incoming HTTP request
     */
    private void logRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String remoteAddr = getClientIpAddress(request);
        
        // Skip logging for actuator endpoints
        if (uri.startsWith("/actuator")) {
            return;
        }
        
        // Build request info
        StringBuilder logMessage = new StringBuilder();
        logMessage.append(String.format(">>> INCOMING REQUEST | %s %s", method, uri));
        
        if (queryString != null && !queryString.isEmpty()) {
            logMessage.append("?").append(queryString);
        }
        
        logMessage.append(String.format(" | client=%s", remoteAddr));
        
        // Log headers (only important ones)
        logMessage.append(" | headers={");
        Map<String, String> headers = getImportantHeaders(request);
        headers.forEach((key, value) -> 
            logMessage.append(key).append("=").append(value).append(", ")
        );
        logMessage.append("}");
        
        log.info(logMessage.toString());
        
        // Log request parameters (for GET/DELETE)
        if ("GET".equals(method) || "DELETE".equals(method)) {
            Map<String, String[]> params = request.getParameterMap();
            if (!params.isEmpty()) {
                log.debug("Request parameters: {}", params);
            }
        }
    }
    
    /**
     * Log outgoing HTTP response
     */
    private void logResponse(HttpServletRequest request, 
                            HttpServletResponse response, 
                            long duration) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        
        // Skip logging for actuator endpoints
        if (uri.startsWith("/actuator")) {
            return;
        }
        
        // Determine log level based on status code
        String statusEmoji = getStatusEmoji(status);
        
        String logMessage = String.format(
            "<<< OUTGOING RESPONSE | %s %s | status=%d %s | duration=%s",
            method, uri, status, statusEmoji, LoggingUtils.formatDuration(duration)
        );
        
        // Log at appropriate level
        if (status >= 500) {
            log.error(logMessage);
        } else if (status >= 400) {
            log.warn(logMessage);
        } else {
            log.info(logMessage);
        }
        
        // Log slow requests (> 5 seconds) at WARN level
        if (duration > 5000) {
            log.warn("⚠️  SLOW REQUEST DETECTED | {} {} | duration={} | Consider optimization",
                method, uri, LoggingUtils.formatDuration(duration));
        }
    }
    
    /**
     * Get client IP address (handles proxy headers)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // If multiple IPs, take the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Get important headers for logging
     */
    private Map<String, String> getImportantHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        
        String[] importantHeaders = {
            "Content-Type",
            "Content-Length",
            "User-Agent",
            "Accept",
            REQUEST_ID_HEADER,
            USER_ID_HEADER
        };
        
        for (String headerName : importantHeaders) {
            String headerValue = request.getHeader(headerName);
            if (headerValue != null) {
                headers.put(headerName, headerValue);
            }
        }
        
        return headers;
    }
    
    /**
     * Get emoji based on HTTP status code
     */
    private String getStatusEmoji(int status) {
        if (status >= 200 && status < 300) {
            return "✅";
        } else if (status >= 300 && status < 400) {
            return "↪️";
        } else if (status >= 400 && status < 500) {
            return "⚠️";
        } else {
            return "❌";
        }
    }
}
