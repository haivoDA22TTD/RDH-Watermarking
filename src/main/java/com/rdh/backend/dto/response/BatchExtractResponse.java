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
public class BatchExtractResponse {
    
    /**
     * Tổng số ảnh được xử lý
     */
    private Integer totalImages;
    
    /**
     * Số ảnh giải mã thành công
     */
    private Integer successCount;
    
    /**
     * Số ảnh giải mã thất bại
     */
    private Integer failureCount;
    
    /**
     * Danh sách kết quả chi tiết từng ảnh
     */
    private List<ExtractionResult> results;
    
    /**
     * Thông báo tổng quan
     */
    private String message;
    
    /**
     * Kết quả giải mã từng ảnh
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtractionResult {
        /**
         * Tên file ảnh stego
         */
        private String fileName;
        
        /**
         * Dữ liệu đã giải mã
         */
        private String extractedData;
        
        /**
         * Trạng thái: SUCCESS hoặc FAILED
         */
        private String status;
        
        /**
         * Thông báo lỗi (nếu có)
         */
        private String errorMessage;
        
        /**
         * Kích thước file (bytes)
         */
        private Long fileSize;
    }
}
