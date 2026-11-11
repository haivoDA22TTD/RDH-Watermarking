package com.rdh.backend.dto.request;

import com.rdh.backend.dto.enumtype.PatternType;
import com.rdh.backend.dto.enumtype.WatermarkTemplateType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for watermark template operations.
 * 
 * <p>This DTO is used for embedding different types of watermark templates:
 * <ul>
 *   <li>LOGO - requires logoFile (MultipartFile in controller)</li>
 *   <li>QR_CODE - requires qrText</li>
 *   <li>PATTERN - requires patternType</li>
 * </ul>
 * 
 * <p>Common parameters:
 * <ul>
 *   <li>templateType - Type of watermark template</li>
 *   <li>strength - Watermark strength (0.0-1.0, default 0.5)</li>
 *   <li>size - Size of watermark (pixels, default 100)</li>
 *   <li>positionX - X position (0.0-1.0, 0.5 = center)</li>
 *   <li>positionY - Y position (0.0-1.0, 0.5 = center)</li>
 * </ul>
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatermarkTemplateRequest {
    
    /**
     * Type of watermark template (LOGO, QR_CODE, PATTERN).
     */
    @NotNull(message = "Template type is required")
    private WatermarkTemplateType templateType;
    
    /**
     * Text for QR code generation (required when templateType = QR_CODE).
     */
    private String qrText;
    
    /**
     * Pattern type (required when templateType = PATTERN).
     */
    private PatternType patternType;
    
    /**
     * Watermark strength/alpha (0.0 = transparent, 1.0 = fully opaque).
     * Default: 0.5 (semi-transparent)
     */
    @Min(value = 0, message = "Strength must be between 0.0 and 1.0")
    @Max(value = 1, message = "Strength must be between 0.0 and 1.0")
    private Double strength = 0.5;
    
    /**
     * Size of watermark in pixels.
     * Default: 100 (100x100 pixels)
     */
    @Min(value = 10, message = "Size must be at least 10 pixels")
    @Max(value = 2000, message = "Size must not exceed 2000 pixels")
    private Integer size = 100;
    
    /**
     * X position (0.0 = left, 0.5 = center, 1.0 = right).
     * Default: 0.5 (center)
     */
    @Min(value = 0, message = "Position X must be between 0.0 and 1.0")
    @Max(value = 1, message = "Position X must be between 0.0 and 1.0")
    private Double positionX = 0.5;
    
    /**
     * Y position (0.0 = top, 0.5 = center, 1.0 = bottom).
     * Default: 0.5 (center)
     */
    @Min(value = 0, message = "Position Y must be between 0.0 and 1.0")
    @Max(value = 1, message = "Position Y must be between 0.0 and 1.0")
    private Double positionY = 0.5;
    
    /**
     * Spacing for pattern watermarks (pixels between pattern elements).
     * Default: 50
     */
    @Min(value = 10, message = "Spacing must be at least 10 pixels")
    @Max(value = 500, message = "Spacing must not exceed 500 pixels")
    private Integer spacing = 50;
    
    /**
     * QR code error correction level (L, M, Q, H).
     * Default: M (Medium)
     */
    private String qrErrorCorrection = "M";
}
