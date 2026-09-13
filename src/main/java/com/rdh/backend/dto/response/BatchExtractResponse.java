package com.rdh.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response batch extract - kết quả giải mã nhiều ảnh stego")
public class BatchExtractResponse {
    
    /**
     * Tổng số ảnh được xử lý
     */
    @Schema(description = "Tổng số ảnh được xử lý", example = "3")
    private Integer totalImages;
    
    /**
     * Số ảnh giải mã thành công
     */
    @Schema(description = "Số ảnh extract thành công", example = "3")
    private Integer successCount;
    
    /**
     * Số ảnh giải mã thất bại
     */
    @Schema(description = "Số ảnh extract thất bại", example = "0")
    private Integer failureCount;
    
    /**
     * Danh sách kết quả chi tiết từng ảnh
     */
    @Schema(description = "Danh sách kết quả chi tiết từng ảnh")
    private List<ExtractionResult> results;
    
    /**
     * Thông báo tổng quan
     */
    @Schema(description = "Thông báo tổng quan", example = "Extracted 3 images: 3 success, 0 failed")
    private String message;
    
    /**
     * Kết quả giải mã từng ảnh
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Kết quả giải mã từng ảnh trong batch")
    public static class ExtractionResult {
        /**
         * Tên file ảnh stego
         */
        @Schema(description = "Tên file ảnh stego", example = "stego_image1.png")
        private String fileName;
        
        /**
         * Dữ liệu đã giải mã
         */
        @Schema(description = "Dữ liệu text đã giải mã", example = "Batch secret")
        private String extractedData;
        
        /**
         * Trạng thái: SUCCESS hoặc FAILED
         */
        @Schema(description = "Trạng thái giải mã", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED"})
        private String status;
        
        /**
         * Thông báo lỗi (nếu có)
         */
        @Schema(description = "Thông báo lỗi (nếu có)", example = "null", nullable = true)
        private String errorMessage;
        
        /**
         * Kích thước file (bytes)
         */
        @Schema(description = "Kích thước file (bytes)", example = "123456")
        private Long fileSize;
    }
}
