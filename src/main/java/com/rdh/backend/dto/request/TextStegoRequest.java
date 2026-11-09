package com.rdh.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for text steganography embed operation.
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for embedding secret data into text")
public class TextStegoRequest {
    
    @NotBlank(message = "Cover text is required")
    @Size(min = 10, message = "Cover text must be at least 10 characters")
    @Schema(
        description = "Cover text to hide secret data in. Can be any text format: plain text, Markdown, JSON, code, HTML, etc.",
        example = "This is a sample text that will contain hidden data. It can be any length and format.",
        required = true
    )
    private String coverText;
    
    @NotBlank(message = "Secret data is required")
    @Size(min = 1, max = 10000, message = "Secret data must be between 1 and 10000 characters")
    @Schema(
        description = "Secret data to hide (max 10000 characters)",
        example = "This is my secret message",
        required = true
    )
    private String secretData;
    
    @Schema(
        description = "Optional metadata about the text format",
        example = "json"
    )
    private String format;
}
