package com.rdh.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {
    
    /**
     * PSNR (Peak Signal-to-Noise Ratio) - Đánh giá độ lệch màu ảnh
     * Đơn vị: dB (decibel)
     * Giá trị cao hơn = chất lượng tốt hơn
     * Thông thường: > 30 dB là tốt, > 40 dB là rất tốt
     */
    private Double psnr;
    
    /**
     * Capacity - Dung lượng nhúng tối đa
     * Số bit tối đa có thể nhúng vào ảnh
     */
    private Long capacity;
    
    /**
     * SSIM (Structural Similarity Index) - Đánh giá độ tương đồng cấu trúc thị giác
     * Giá trị trong khoảng [0, 1]
     * 1.0 = giống hệt nhau, 0.0 = hoàn toàn khác
     * > 0.95 là rất tốt cho RDH
     */
    private Double ssim;
    
    /**
     * Kích thước ảnh
     */
    private Integer width;
    private Integer height;
    
    /**
     * Thông tin bổ sung
     */
    private String message;
}
