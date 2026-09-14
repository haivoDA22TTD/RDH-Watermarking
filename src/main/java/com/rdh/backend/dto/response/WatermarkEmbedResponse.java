package com.rdh.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response sau khi embed watermark thành công
 * Trả về thông tin metadata về quá trình embedding
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response chứa metadata của watermark embedding")
public class WatermarkEmbedResponse {
    
    @Schema(description = "Thuật toán đã sử dụng", example = "DCT")
    private String algorithm;
    
    @Schema(description = "Độ mạnh đã áp dụng", example = "50")
    private Integer strength;
    
    @Schema(description = "Số bits watermark đã nhúng", example = "256")
    private Integer embeddedBits;
    
    @Schema(description = "PSNR của ảnh watermarked so với ảnh gốc (dB)", example = "42.5")
    private Double psnr;
    
    @Schema(description = "SSIM của ảnh watermarked so với ảnh gốc", example = "0.985")
    private Double ssim;
    
    @Schema(description = "Kích thước ảnh gốc (bytes)", example = "102400")
    private Long originalSize;
    
    @Schema(description = "Kích thước ảnh watermarked (bytes)", example = "105600")
    private Long watermarkedSize;
    
    @Schema(description = "Thời gian xử lý (milliseconds)", example = "250")
    private Long processingTimeMs;
    
    @Schema(description = "Thông báo kết quả", example = "Watermark embedded successfully")
    private String message;
}
