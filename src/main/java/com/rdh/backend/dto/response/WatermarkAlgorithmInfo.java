package com.rdh.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thông tin chi tiết về một thuật toán watermarking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết về thuật toán watermarking")
public class WatermarkAlgorithmInfo {
    
    @Schema(description = "Tên thuật toán", example = "DCT")
    private String name;
    
    @Schema(description = "Tên hiển thị", example = "DCT-based Watermarking")
    private String displayName;
    
    @Schema(description = "Mô tả thuật toán", example = "Robust against JPEG compression and resizing")
    private String description;
    
    @Schema(description = "Mức độ bền", example = "HIGH", allowableValues = {"LOW", "MEDIUM", "HIGH", "VERY_HIGH"})
    private String robustness;
    
    @Schema(description = "Capacity điển hình (bits)", example = "256")
    private Integer typicalCapacity;
    
    @Schema(description = "PSNR điển hình (dB)", example = "40-50")
    private String typicalPsnr;
    
    @Schema(description = "Các attacks có thể chống", example = "JPEG compression, resizing, light noise")
    private String resistantTo;
    
    @Schema(description = "Các attacks yếu", example = "Heavy cropping, rotation")
    private String vulnerableTo;
    
    @Schema(description = "Có được khuyến nghị không", example = "true")
    private Boolean recommended;
    
    @Schema(description = "Use cases phù hợp", example = "Copyright protection, social media images")
    private String useCases;
}
