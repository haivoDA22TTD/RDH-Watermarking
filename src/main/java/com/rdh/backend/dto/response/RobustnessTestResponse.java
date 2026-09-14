package com.rdh.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response cho robustness testing - đánh giá độ bền watermark
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response chứa kết quả test độ bền watermark với các attacks")
public class RobustnessTestResponse {
    
    @Schema(description = "PSNR giữa ảnh gốc và ảnh watermarked (dB)", example = "42.5")
    private Double psnr;
    
    @Schema(description = "SSIM giữa ảnh gốc và ảnh watermarked", example = "0.985")
    private Double ssim;
    
    @Schema(description = "NCC (Normalized Cross-Correlation) của watermark", example = "0.992")
    private Double ncc;
    
    @Schema(description = "Tổng số attacks đã test", example = "8")
    private Integer totalAttacks;
    
    @Schema(description = "Số attacks passed (watermark survive)", example = "6")
    private Integer passedAttacks;
    
    @Schema(description = "Số attacks failed (watermark mất)", example = "2")
    private Integer failedAttacks;
    
    @Schema(description = "Tỉ lệ thành công (%)", example = "75.0")
    private Double successRate;
    
    @Schema(description = "Kết quả chi tiết từng attack")
    private List<AttackResult> attackResults;
    
    @Schema(description = "Thông báo tổng quan", example = "Robustness test completed: 6/8 attacks passed")
    private String message;
    
    /**
     * Kết quả test từng attack
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Kết quả test từng loại attack")
    public static class AttackResult {
        
        @Schema(description = "Tên attack", example = "JPEG_70")
        private String attackType;
        
        @Schema(description = "Mô tả attack", example = "JPEG Compression 70%")
        private String attackDescription;
        
        @Schema(description = "Mức độ nghiêm trọng", example = "MEDIUM", allowableValues = {"LIGHT", "MEDIUM", "HEAVY"})
        private String severity;
        
        @Schema(description = "Kết quả: passed = watermark survive", example = "true")
        private Boolean passed;
        
        @Schema(description = "Watermark đã extract (nếu có)", example = "COPYRIGHT_2024_JOHN")
        private String extractedWatermark;
        
        @Schema(description = "Bit Error Rate sau attack", example = "0.03")
        private Double ber;
        
        @Schema(description = "Độ tin cậy extraction", example = "0.95")
        private Double confidence;
        
        @Schema(description = "PSNR của ảnh sau attack so với ảnh watermarked", example = "32.5")
        private Double psnr;
        
        @Schema(description = "Thông báo kết quả", example = "Watermark survived JPEG compression")
        private String message;
    }
}
