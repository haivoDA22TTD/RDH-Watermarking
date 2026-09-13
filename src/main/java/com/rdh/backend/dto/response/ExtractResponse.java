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
@Schema(description = "Response chứa dữ liệu đã được giải mã từ ảnh stego")
public class ExtractResponse {
    @Schema(description = "Dữ liệu text đã được giải mã từ ảnh stego", example = "My secret message")
    private String extractedData;
}