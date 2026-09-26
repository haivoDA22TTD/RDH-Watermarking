package com.rdh.backend.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service
 * 
 * Service để quản lý rate limiting sử dụng Token Bucket Algorithm (Bucket4j).
 * 
 * Token Bucket Algorithm:
 * - Mỗi user có một "bucket" chứa tokens
 * - Mỗi request tiêu tốn 1 token
 * - Tokens được refill theo thời gian
 * - Nếu hết tokens → request bị reject
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-09-22
 */
@Service
public class RateLimitService {

    /**
     * Cache lưu trữ buckets cho mỗi key (IP address hoặc user ID)
     */
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    /**
     * Resolve bucket cho một key cụ thể.
     * Nếu chưa có bucket → tạo mới.
     * Nếu đã có → return bucket cũ.
     * 
     * @param key Unique identifier (IP address, user ID, API key, etc.)
     * @return Bucket instance
     */
    public Bucket resolveBucket(String key) {
        return cache.computeIfAbsent(key, k -> createNewBucket());
    }

    /**
     * Tạo bucket mới với rate limit configuration.
     * 
     * Configuration hiện tại:
     * - Capacity: 100 tokens
     * - Refill: 100 tokens mỗi 1 phút
     * 
     * Nghĩa là:
     * - User có thể gọi tối đa 100 requests trong 1 phút
     * - Sau 1 phút, số tokens được reset về 100
     * 
     * @return Bucket instance mới
     */
    private Bucket createNewBucket() {
        // Bandwidth configuration
        Bandwidth limit = Bandwidth.classic(
                100,                                  // Capacity: 100 tokens
                Refill.intervally(100, Duration.ofMinutes(1)) // Refill 100 tokens mỗi 1 phút
        );
        
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Tạo bucket với custom configuration cho specific use case.
     * 
     * @param capacity Số lượng tokens tối đa
     * @param refillTokens Số tokens được refill
     * @param refillDuration Khoảng thời gian refill
     * @return Bucket instance mới
     */
    public Bucket createCustomBucket(long capacity, long refillTokens, Duration refillDuration) {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, refillDuration)
        );
        
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Check xem một key có bị rate limit không (không consume token).
     * 
     * @param key Unique identifier
     * @return true nếu còn tokens available, false nếu hết tokens
     */
    public boolean isAllowed(String key) {
        Bucket bucket = resolveBucket(key);
        return bucket.tryConsume(1);
    }

    /**
     * Lấy số tokens còn lại của một key.
     * 
     * @param key Unique identifier
     * @return Số tokens còn lại
     */
    public long getAvailableTokens(String key) {
        Bucket bucket = resolveBucket(key);
        return bucket.getAvailableTokens();
    }

    /**
     * Reset bucket của một key (clear rate limit).
     * 
     * @param key Unique identifier
     */
    public void resetBucket(String key) {
        cache.remove(key);
    }

    /**
     * Clear tất cả buckets (dùng cho testing hoặc maintenance).
     */
    public void clearAllBuckets() {
        cache.clear();
    }

    /**
     * Get total số keys đang được track.
     * 
     * @return Số lượng unique keys
     */
    public int getTotalKeys() {
        return cache.size();
    }
}
