package com.rdh.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response DTO for text steganography operations.
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response from text steganography operations")
public class TextStegoResponse {
    
    @Schema(description = "Operation success status", example = "true")
    private boolean success;
    
    @Schema(description = "Result message", example = "Secret data embedded successfully")
    private String message;
    
    @Schema(
        description = "Stego text with embedded secret data (for embed operation) or cleaned text (for clean operation)",
        example = "This​ ​is​ ​a​ ​sample​ ​text..."
    )
    private String stegoText;
    
    @Schema(
        description = "Extracted secret data (for extract operation)",
        example = "This is my secret message"
    )
    private String secretData;
    
    @Schema(
        description = "Metadata about the operation",
        example = "{\"algorithm\": \"Unicode Zero-Width Characters\", \"capacity\": 100, \"usage\": \"25.5%\"}"
    )
    private Map<String, Object> metadata;
}
