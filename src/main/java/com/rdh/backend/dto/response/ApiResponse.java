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

    /**
     * Tạo response thành công với custom message.
     * 
     * @param message Custom message
     * @param data Dữ liệu trả về
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Tạo response lỗi.
     * 
     * @param code HTTP status code
     * @param message Error message
     * @return ApiResponse instance
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .message(message)
                .data(null)
                .build();
    }
}