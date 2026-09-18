package com.rdh.backend.exception;

import com.rdh.backend.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for all REST API exceptions
 * Handles validation errors, custom exceptions, and system errors
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle custom InvalidImageException
    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidImageException(InvalidImageException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(400)
                .message("Invalid image: " + ex.getMessage())
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. Handle custom InsufficientCapacityException
    @ExceptionHandler(InsufficientCapacityException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInsufficientCapacityException(
            InsufficientCapacityException ex) {
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("error", "INSUFFICIENT_CAPACITY");
        errorDetails.put("requiredCapacity", ex.getRequiredCapacity());
        errorDetails.put("availableCapacity", ex.getAvailableCapacity());
        errorDetails.put("deficit", ex.getDeficit());
        
        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder()
                .code(400)
                .message(ex.getMessage())
                .data(errorDetails)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 3. Handle custom WatermarkNotFoundException
    @ExceptionHandler(WatermarkNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleWatermarkNotFoundException(
            WatermarkNotFoundException ex) {
        
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("error", "WATERMARK_NOT_FOUND");
        if (ex.getReason() != null) {
            errorDetails.put("reason", ex.getReason());
        }
        
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .code(404)
                .message(ex.getMessage())
                .data(errorDetails)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // 4. Handle custom UnsupportedAlgorithmException
    @ExceptionHandler(UnsupportedAlgorithmException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleUnsupportedAlgorithmException(
            UnsupportedAlgorithmException ex) {
        
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("error", "UNSUPPORTED_ALGORITHM");
        if (ex.getAlgorithmName() != null) {
            errorDetails.put("algorithmName", ex.getAlgorithmName());
        }
        if (ex.getAvailableAlgorithms() != null) {
            errorDetails.put("availableAlgorithms", ex.getAvailableAlgorithms());
        }
        
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .code(400)
                .message(ex.getMessage())
                .data(errorDetails)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 5. Handle generic RdhException (fallback for other custom exceptions)
    @ExceptionHandler(RdhException.class)
    public ResponseEntity<ApiResponse<Void>> handleRdhException(RdhException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(400)
                .message(ex.getMessage())
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 6. Handle Bean Validation errors for @RequestBody (DTO validation)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        String mainMessage = String.format("Validation failed: %d error(s)", errors.size());
        
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .code(400)
                .message(mainMessage)
                .data(errors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 7. Handle Bean Validation errors for @RequestParam and @PathVariable
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                    violation -> violation.getPropertyPath().toString(),
                    ConstraintViolation::getMessage
                ));
        
        String mainMessage = String.format("Validation failed: %d error(s)", errors.size());
        
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .code(400)
                .message(mainMessage)
                .data(errors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 8. Handle file upload size exceeded
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {
        
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("error", "FILE_TOO_LARGE");
        errorDetails.put("maxSize", "200MB");
        
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .code(413)
                .message("File upload size exceeded. Maximum allowed: 200MB")
                .data(errorDetails)
                .build();
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    // 9. Handle IllegalArgumentException (e.g., invalid enum values)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(400)
                .message("Invalid argument: " + ex.getMessage())
                .data(null)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 10. Handle generic Exception (fallback for unexpected errors)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleGeneralException(Exception ex) {
        
        // Log full stack trace for debugging
        System.err.println("=== UNEXPECTED ERROR ===");
        ex.printStackTrace();
        
        Map<String, String> errorDetails = new HashMap<>();
        errorDetails.put("errorType", ex.getClass().getSimpleName());
        errorDetails.put("errorMessage", ex.getMessage());
        
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .code(500)
                .message("Internal server error occurred. Please contact administrator.")
                .data(errorDetails)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}