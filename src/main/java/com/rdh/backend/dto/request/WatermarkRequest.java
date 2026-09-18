package com.rdh.backend.dto.request;

import com.rdh.backend.dto.enumtype.WatermarkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
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
    
    @NotBlank(message = "Watermark text is required and cannot be blank")
    @Size(min = 1, max = 64, message = "Watermark text must be between 1 and 64 characters")
    @Pattern(
        regexp = "^[ -~]+$",
        message = "Watermark text must contain only printable ASCII characters (space to tilde)"
    )
    @Schema(
            description = "Dữ liệu watermark (text) - sẽ được convert sang binary",
            example = "COPYRIGHT_2024_JOHN",
            maxLength = 64,
            required = true
    )
    private String watermarkText;
    
    @NotNull(message = "Watermark algorithm is required")
    @Schema(
            description = "Thuật toán watermarking",
            example = "DCT",
            allowableValues = {"DCT", "DWT", "LSB_ROBUST"},
            defaultValue = "DCT"
    )
    private WatermarkType algorithm;
    
    @NotNull(message = "Watermark strength is required")
    @Min(value = 0, message = "Watermark strength must be at least 0")
    @Max(value = 100, message = "Watermark strength must not exceed 100")
    @Schema(
            description = "Độ mạnh của watermark (0-100). Cao hơn = bền hơn nhưng PSNR thấp hơn",
            example = "50",
            minimum = "0",
            maximum = "100",
            defaultValue = "50"
    )
    private Integer strength;
}
