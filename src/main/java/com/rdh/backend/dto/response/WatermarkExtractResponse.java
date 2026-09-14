package com.rdh.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response sau khi extract watermark từ ảnh
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response chứa watermark đã được extract")
public class WatermarkExtractResponse {
    
    @Schema(description = "Dữ liệu watermark đã extract (text)", example = "COPYRIGHT_2024_JOHN")
    private String extractedWatermark;
    
    @Schema(description = "Thuật toán đã sử dụng", example = "DCT")
    private String algorithm;
    
    @Schema(description = "Độ tin cậy của extraction (0-1). > 0.9 là tốt", example = "0.98")
    private Double confidence;
    
    @Schema(description = "Bit Error Rate - tỉ lệ bit lỗi (0-1). < 0.05 là tốt", example = "0.02")
    private Double ber;
    
    @Schema(description = "Số bits đã extract", example = "256")
    private Integer extractedBits;
    
    @Schema(description = "Thời gian xử lý (milliseconds)", example = "180")
    private Long processingTimeMs;
    
    @Schema(description = "Trạng thái extraction", example = "SUCCESS", allowableValues = {"SUCCESS", "PARTIAL", "FAILED"})
    private String status;
    
    @Schema(description = "Thông báo kết quả", example = "Watermark extracted successfully")
    private String message;
}
