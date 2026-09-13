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
@Schema(description = "Response wrapper chuẩn cho tất cả API endpoints")
public class ApiResponse<T> {
    
    @Schema(description = "HTTP status code", example = "200")
    private int code;
    
    @Schema(description = "Thông báo kết quả", example = "Thao tác thành công")
    private String message;
    
    @Schema(description = "Dữ liệu trả về (generic type)")
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message("Thao tác thành công")
                .data(data)
                .build();
    }
}