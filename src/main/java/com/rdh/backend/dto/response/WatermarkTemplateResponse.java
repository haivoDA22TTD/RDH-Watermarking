package com.rdh.backend.dto.response;

import com.rdh.backend.dto.enumtype.PatternType;
import com.rdh.backend.dto.enumtype.WatermarkTemplateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Response DTO for watermark template operations.
 * 
 * <p>Contains information about the watermark template embedding result:
 * <ul>
 *   <li>success - Whether operation was successful</li>
 *   <li>message - Success/error message</li>
 *   <li>templateType - Type of watermark used</li>
 *   <li>metadata - Additional information (size, position, quality metrics, etc.)</li>
 * </ul>
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatermarkTemplateResponse {
    
    /**
     * Whether the operation was successful.
     */
    private boolean success;
    
    /**
     * Success or error message.
     */
    private String message;
    
    /**
     * Type of watermark template used.
     */
    private WatermarkTemplateType templateType;
    
    /**
     * Pattern type (if templateType = PATTERN).
     */
    private PatternType patternType;
    
    /**
     * QR code text (if templateType = QR_CODE).
     */
    private String qrText;
    
    /**
     * Additional metadata about the watermark operation.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * Helper method to add metadata.
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }
    
    /**
     * Create a success response.
     */
    public static WatermarkTemplateResponse success(String message, WatermarkTemplateType templateType) {
        return WatermarkTemplateResponse.builder()
                .success(true)
                .message(message)
                .templateType(templateType)
                .metadata(new HashMap<>())
                .build();
    }
    
    /**
     * Create an error response.
     */
    public static WatermarkTemplateResponse error(String message, WatermarkTemplateType templateType) {
        return WatermarkTemplateResponse.builder()
                .success(false)
                .message(message)
                .templateType(templateType)
                .metadata(new HashMap<>())
                .build();
    }
}
