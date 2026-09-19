package com.rdh.backend.util;

import org.slf4j.MDC;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utility class for logging operations
 * Provides helper methods for structured logging, MDC management, and data masking
 */
public class LoggingUtils {
    
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";
    private static final String SESSION_ID_KEY = "sessionId";
    
    /**
     * Generate a unique request ID for request tracking
     */
    public static String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * Set request ID in MDC (Mapped Diagnostic Context)
     */
    public static void setRequestId(String requestId) {
        if (requestId != null && !requestId.isEmpty()) {
            MDC.put(REQUEST_ID_KEY, requestId);
        }
    }
    
    /**
     * Get request ID from MDC
     */
    public static String getRequestId() {
        return MDC.get(REQUEST_ID_KEY);
    }
    
    /**
     * Set user ID in MDC for user tracking
     */
    public static void setUserId(String userId) {
        if (userId != null && !userId.isEmpty()) {
            MDC.put(USER_ID_KEY, userId);
        }
    }
    
    /**
     * Set session ID in MDC
     */
    public static void setSessionId(String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            MDC.put(SESSION_ID_KEY, sessionId);
        }
    }
    
    /**
     * Clear all MDC context (call at end of request)
     */
    public static void clearMDC() {
        MDC.clear();
    }
    
    /**
     * Format file information for logging
     */
    public static String formatFileInfo(MultipartFile file) {
        if (file == null) {
            return "null";
        }
        
        return String.format("%s (size=%s, type=%s)",
            maskSensitiveData(file.getOriginalFilename()),
            formatFileSize(file.getSize()),
            file.getContentType()
        );
    }
    
    /**
     * Format multiple files information
     */
    public static String formatFilesInfo(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return "[]";
        }
        
        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        
        return String.format("[count=%d, totalSize=%s]",
            files.size(),
            formatFileSize(totalSize)
        );
    }
    
    /**
     * Format file size in human-readable format
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
    
    /**
     * Format duration in human-readable format
     */
    public static String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.2fs", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }
    
    /**
     * Mask sensitive data in strings (email, username, etc.)
     */
    public static String maskSensitiveData(String data) {
        if (data == null || data.isEmpty()) {
            return "null";
        }
        
        // Mask email addresses
        if (data.contains("@")) {
            String[] parts = data.split("@");
            if (parts.length == 2) {
                String username = parts[0];
                String maskedUsername = username.length() <= 2 
                    ? "**" 
                    : username.substring(0, 2) + "***";
                return maskedUsername + "@" + parts[1];
            }
        }
        
        // Mask long strings (keep first and last 4 chars)
        if (data.length() > 20) {
            return data.substring(0, 4) + "..." + data.substring(data.length() - 4);
        }
        
        return data;
    }
    
    /**
     * Truncate long text for logging
     */
    public static String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "... (truncated)";
    }
    
    /**
     * Create a performance log message
     */
    public static String performanceLog(String operation, long durationMs, String details) {
        return String.format("PERFORMANCE | operation=%s | duration=%s | %s",
            operation,
            formatDuration(durationMs),
            details
        );
    }
    
    /**
     * Create a structured log message with key-value pairs
     */
    public static String structuredLog(String message, Object... keyValuePairs) {
        if (keyValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("Key-value pairs must be even");
        }
        
        StringBuilder sb = new StringBuilder(message);
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            sb.append(" | ").append(keyValuePairs[i]).append("=").append(keyValuePairs[i + 1]);
        }
        
        return sb.toString();
    }
}
