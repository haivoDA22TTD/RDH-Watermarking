package com.rdh.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho RDH embedding (LSB, Histogram Shifting)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request để nhúng secret data vào ảnh sử dụng RDH")
public class EmbedRequest {
    
    @NotBlank(message = "Secret data is required and cannot be blank")
    @Size(min = 1, max = 512, message = "Secret data must be between 1 and 512 characters")
    @Pattern(
        regexp = "^[ -~]+$",
        message = "Secret data must contain only printable ASCII characters (space to tilde)"
    )
    @Schema(
            description = "Dữ liệu bí mật cần nhúng (text hoặc hex string)",
            example = "SECRET_MESSAGE_2024",
            maxLength = 512,
            required = true
    )
    private String secretData;
}