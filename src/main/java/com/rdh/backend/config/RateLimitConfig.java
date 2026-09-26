package com.rdh.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting Configuration
 * 
 * Cấu hình cache cho rate limiting sử dụng Caffeine (in-memory cache).
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-09-22
 */
@Configuration
@EnableCaching
public class RateLimitConfig {

    /**
     * Cấu hình Caffeine Cache Manager cho rate limiting.
     * 
     * Caffeine là high-performance in-memory cache tương thích với Spring Cache.
     * 
     * @return CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("rateLimitBuckets");
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // Maximum 10,000 entries (10,000 unique IPs/users)
                .maximumSize(10_000)
                
                // Entry expires after 1 hour of no access
                .expireAfterAccess(Duration.ofHours(1))
                
                // Record cache statistics for monitoring
                .recordStats()
        );
        
        return cacheManager;
    }
}
