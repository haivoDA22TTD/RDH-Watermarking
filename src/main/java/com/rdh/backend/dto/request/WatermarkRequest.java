package com.rdh.backend.dto.request;

import com.rdh.backend.dto.enumtype.WatermarkType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho watermark embedding
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request để nhúng watermark vào ảnh")
public class WatermarkRequest {
    
    @Schema(
            description = "Dữ liệu watermark (text) - sẽ được convert sang binary",
            example = "COPYRIGHT_2024_JOHN",
            maxLength = 64,
            required = true
    )
    private String watermarkText;
    
    @Schema(
            description = "Thuật toán watermarking",
            example = "DCT",
            allowableValues = {"DCT", "DWT", "LSB_ROBUST"},
            defaultValue = "DCT"
    )
    private WatermarkType algorithm;
    
    @Schema(
            description = "Độ mạnh của watermark (0-100). Cao hơn = bền hơn nhưng PSNR thấp hơn",
            example = "50",
            minimum = "0",
            maximum = "100",
            defaultValue = "50"
    )
    private Integer strength;
}
