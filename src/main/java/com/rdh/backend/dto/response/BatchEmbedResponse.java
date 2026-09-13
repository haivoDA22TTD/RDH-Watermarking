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
@Schema(description = "Response batch embed - kết quả xử lý nhiều ảnh embed")
public class BatchEmbedResponse {
    
    /**
     * Tổng số ảnh được xử lý
     */
    @Schema(description = "Tổng số ảnh được xử lý", example = "5")
    private Integer totalImages;
    
    /**
     * Số ảnh thành công
     */
    @Schema(description = "Số ảnh embed thành công", example = "5")
    private Integer successCount;
    
    /**
     * Số ảnh thất bại
     */
    @Schema(description = "Số ảnh embed thất bại", example = "0")
    private Integer failureCount;
    
    /**
     * Danh sách kết quả chi tiết từng ảnh
     */
    @Schema(description = "Danh sách kết quả chi tiết từng ảnh")
    private List<ImageResult> results;
    
    /**
     * Thông báo tổng quan
     */
    @Schema(description = "Thông báo tổng quan", example = "Embedded 5 images: 5 success, 0 failed")
    private String message;
    
    /**
     * Kết quả xử lý từng ảnh
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Kết quả xử lý từng ảnh trong batch")
    public static class ImageResult {
        /**
         * Tên file gốc
         */
        @Schema(description = "Tên file gốc", example = "image1.png")
        private String originalFileName;
        
        /**
         * Tên file stego trong ZIP
         */
        @Schema(description = "Tên file stego trong ZIP", example = "stego_image1.png")
        private String stegoFileName;
        
        /**
         * Trạng thái: SUCCESS hoặc FAILED
         */
        @Schema(description = "Trạng thái xử lý", example = "SUCCESS", allowableValues = {"SUCCESS", "FAILED"})
        private String status;
        
        /**
         * Thông báo lỗi (nếu có)
         */
        @Schema(description = "Thông báo lỗi (nếu có)", example = "null", nullable = true)
        private String errorMessage;
        
        /**
         * Kích thước file gốc (bytes)
         */
        @Schema(description = "Kích thước file gốc (bytes)", example = "102400")
        private Long originalSize;
        
        /**
         * Kích thước file stego (bytes)
         */
        @Schema(description = "Kích thước file stego (bytes)", example = "102456")
        private Long stegoSize;
    }
}
