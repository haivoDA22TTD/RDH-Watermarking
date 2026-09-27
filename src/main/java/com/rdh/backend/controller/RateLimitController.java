package com.rdh.backend.controller;

import com.rdh.backend.dto.response.ApiResponse;
import com.rdh.backend.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Rate Limit Management Controller
 * 
 * Controller để quản lý và monitor rate limiting.
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-09-22
 */
@RestController
@RequestMapping("/api/v1/rate-limit")
@Tag(name = "Rate Limit Management", description = "APIs để quản lý rate limiting")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    public RateLimitController(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    /**
     * Kiểm tra rate limit status của client hiện tại.
     * 
     * @param request HTTP request để lấy client IP
     * @return Rate limit information
     */
    @GetMapping("/status")
    @Operation(
            summary = "Check Rate Limit Status",
            description = "Kiểm tra số requests còn lại của client hiện tại"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStatus(HttpServletRequest request) {
        String clientId = getClientId(request);
        long availableTokens = rateLimitService.getAvailableTokens(clientId);
        
        Map<String, Object> data = new HashMap<>();
        data.put("clientId", clientId);
        data.put("availableTokens", availableTokens);
        data.put("limit", 100);
        data.put("used", 100 - availableTokens);
        data.put("resetPeriod", "1 minute");
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Reset rate limit của một client cụ thể.
     * 
     * @param clientId Client identifier (IP address)
     * @return Success message
     */
    @DeleteMapping("/reset/{clientId}")
    @Operation(
            summary = "Reset Rate Limit",
            description = "Reset rate limit bucket của một client cụ thể (Admin only)"
    )
    public ResponseEntity<ApiResponse<String>> resetRateLimit(
            @Parameter(description = "Client ID (IP address) cần reset")
            @PathVariable String clientId) {
        
        rateLimitService.resetBucket(clientId);
        
        return ResponseEntity.ok(
                ApiResponse.success("Rate limit reset successfully for client: " + clientId)
        );
    }

    /**
     * Reset rate limit của client hiện tại.
     * 
     * @param request HTTP request để lấy client IP
     * @return Success message
     */
    @DeleteMapping("/reset")
    @Operation(
            summary = "Reset My Rate Limit",
            description = "Reset rate limit của client hiện tại"
    )
    public ResponseEntity<ApiResponse<String>> resetMyRateLimit(HttpServletRequest request) {
        String clientId = getClientId(request);
        rateLimitService.resetBucket(clientId);
        
        return ResponseEntity.ok(
                ApiResponse.success("Your rate limit has been reset")
        );
    }

    /**
     * Clear tất cả rate limit buckets.
     * 
     * @return Success message
     */
    @DeleteMapping("/clear-all")
    @Operation(
            summary = "Clear All Rate Limits",
            description = "Clear tất cả rate limit buckets (Admin only - use with caution)"
    )
    public ResponseEntity<ApiResponse<String>> clearAllRateLimits() {
        rateLimitService.clearAllBuckets();
        
        return ResponseEntity.ok(
                ApiResponse.success("All rate limit buckets cleared")
        );
    }

    /**
     * Lấy thống kê rate limiting.
     * 
     * @return Statistics
     */
    @GetMapping("/stats")
    @Operation(
            summary = "Get Rate Limit Statistics",
            description = "Lấy thống kê về rate limiting (Admin only)"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        int totalKeys = rateLimitService.getTotalKeys();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTrackedClients", totalKeys);
        stats.put("maxClients", 10000);
        stats.put("cacheUtilization", String.format("%.2f%%", (totalKeys / 10000.0) * 100));
        
        // Rate limit config
        Map<String, Object> rateLimitConfig = new HashMap<>();
        rateLimitConfig.put("capacity", 100);
        rateLimitConfig.put("refillPeriod", "1 minute");
        rateLimitConfig.put("refillTokens", 100);
        stats.put("rateLimitConfig", rateLimitConfig);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Get client identifier từ request.
     * 
     * @param request HTTP request
     * @return Client identifier (IP address)
     */
    private String getClientId(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
