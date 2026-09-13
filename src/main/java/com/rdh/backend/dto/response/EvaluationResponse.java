package com.rdh.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response chứa các metrics đánh giá chất lượng ảnh stego")
public class EvaluationResponse {
    
    /**
     * PSNR (Peak Signal-to-Noise Ratio) - Đánh giá độ lệch màu ảnh
     * Đơn vị: dB (decibel)
     * Giá trị cao hơn = chất lượng tốt hơn
     * Thông thường: > 30 dB là tốt, > 40 dB là rất tốt
     */
    @Schema(description = "PSNR (Peak Signal-to-Noise Ratio) đánh giá độ nhiễu, đơn vị dB. > 50 dB là xuất sắc", example = "57.88")
    private Double psnr;
    
    /**
     * Capacity - Dung lượng nhúng tối đa
     * Số bit tối đa có thể nhúng vào ảnh
     */
    @Schema(description = "Capacity - Số bit tối đa có thể nhúng vào ảnh", example = "5396")
    private Long capacity;
    
    /**
     * SSIM (Structural Similarity Index) - Đánh giá độ tương đồng cấu trúc thị giác
     * Giá trị trong khoảng [0, 1]
     * 1.0 = giống hệt nhau, 0.0 = hoàn toàn khác
     * > 0.95 là rất tốt cho RDH
     */
    @Schema(description = "SSIM (Structural Similarity Index) đánh giá độ tương đồng cấu trúc, giá trị 0-1. > 0.99 là xuất sắc", example = "0.9999")
    private Double ssim;
    
    /**
     * Kích thước ảnh
     */
    @Schema(description = "Chiều rộng ảnh (pixels)", example = "450")
    private Integer width;
    
    @Schema(description = "Chiều cao ảnh (pixels)", example = "450")
    private Integer height;
    
    /**
     * Thông tin bổ sung
     */
    @Schema(description = "Thông báo kết quả", example = "Đánh giá chất lượng thành công")
    private String message;
}
