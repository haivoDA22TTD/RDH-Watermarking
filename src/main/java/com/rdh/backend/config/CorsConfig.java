package com.rdh.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) Configuration
 * 
 * Cho phép frontend từ các domain khác nhau truy cập backend API.
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-09-22
 */
@Configuration
public class CorsConfig {

    /**
     * Cấu hình CORS filter với các thiết lập bảo mật và linh hoạt.
     * 
     * Production: Nên giới hạn allowedOrigins thành danh sách cụ thể.
     * Development: Có thể dùng "*" cho tiện lợi.
     * 
     * @return CorsFilter đã được cấu hình
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // ============================================
        // 1. Allowed Origins (Nguồn được phép)
        // ============================================
        // Development: cho phép tất cả origins
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // Production: nên chỉ định cụ thể
        // config.setAllowedOrigins(Arrays.asList(
        //     "http://localhost:3000",           // React dev server
        //     "http://localhost:5173",           // Vite dev server  
        //     "http://localhost:4200",           // Angular dev server
        //     "https://your-frontend-domain.com" // Production frontend
        // ));
        
        // ============================================
        // 2. Allowed Methods (HTTP methods được phép)
        // ============================================
        config.setAllowedMethods(Arrays.asList(
            "GET",      // Lấy dữ liệu
            "POST",     // Tạo mới, upload
            "PUT",      // Cập nhật toàn bộ
            "PATCH",    // Cập nhật một phần
            "DELETE",   // Xóa
            "OPTIONS"   // Preflight request
        ));
        
        // ============================================
        // 3. Allowed Headers (Headers được phép gửi)
        // ============================================
        config.setAllowedHeaders(Arrays.asList(
            "Origin",
            "Content-Type",
            "Accept",
            "Authorization",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Total-Images",      // Custom header cho batch processing
            "X-Success-Count",     // Custom header cho batch processing
            "X-Failure-Count"      // Custom header cho batch processing
        ));
        
        // ============================================
        // 4. Exposed Headers (Headers mà browser có thể đọc)
        // ============================================
        config.setExposedHeaders(Arrays.asList(
            "Content-Disposition",  // Cho download file
            "Content-Type",
            "Content-Length",
            "X-Total-Images",       // Custom header cho batch processing
            "X-Success-Count",      // Custom header cho batch processing
            "X-Failure-Count",      // Custom header cho batch processing
            "X-RateLimit-Limit",    // Rate limiting info
            "X-RateLimit-Remaining", // Rate limiting info
            "X-RateLimit-Reset",    // Rate limiting info
            "Retry-After"           // Rate limiting retry time
        ));
        
        // ============================================
        // 5. Allow Credentials (Cho phép gửi cookies/auth)
        // ============================================
        config.setAllowCredentials(true);
        
        // ============================================
        // 6. Max Age (Cache preflight response trong bao lâu)
        // ============================================
        // 3600 giây = 1 giờ (giảm số lượng preflight requests)
        config.setMaxAge(3600L);
        
        // ============================================
        // 7. Apply Configuration
        // ============================================
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        // Áp dụng cho TẤT CẢ endpoints
        source.registerCorsConfiguration("/**", config);
        
        // Hoặc có thể chỉ định cụ thể cho từng path pattern:
        // source.registerCorsConfiguration("/api/**", config);
        // source.registerCorsConfiguration("/actuator/**", config);
        
        return new CorsFilter(source);
    }
    
    /**
     * Alternative: Sử dụng WebMvcConfigurer (nếu không muốn dùng CorsFilter)
     * 
     * Uncomment method này và comment bean corsFilter() ở trên nếu muốn dùng approach này.
     */
    /*
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .exposedHeaders("Content-Disposition", "X-Total-Images", "X-Success-Count", "X-Failure-Count")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
    */
}
