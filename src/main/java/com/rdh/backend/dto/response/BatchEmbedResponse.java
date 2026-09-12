package com.rdh.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchEmbedResponse {
    
    /**
     * Tổng số ảnh được xử lý
     */
    private Integer totalImages;
    
    /**
     * Số ảnh thành công
     */
    private Integer successCount;
    
    /**
     * Số ảnh thất bại
     */
    private Integer failureCount;
    
    /**
     * Danh sách kết quả chi tiết từng ảnh
     */
    private List<ImageResult> results;
    
    /**
     * Thông báo tổng quan
     */
    private String message;
    
    /**
     * Kết quả xử lý từng ảnh
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageResult {
        /**
         * Tên file gốc
         */
        private String originalFileName;
        
        /**
         * Tên file stego trong ZIP
         */
        private String stegoFileName;
        
        /**
         * Trạng thái: SUCCESS hoặc FAILED
         */
        private String status;
        
        /**
         * Thông báo lỗi (nếu có)
         */
        private String errorMessage;
        
        /**
         * Kích thước file gốc (bytes)
         */
        private Long originalSize;
        
        /**
         * Kích thước file stego (bytes)
         */
        private Long stegoSize;
    }
}
