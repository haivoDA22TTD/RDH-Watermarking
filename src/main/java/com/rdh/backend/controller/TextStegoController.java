package com.rdh.backend.controller;

import com.rdh.backend.dto.request.TextStegoRequest;
import com.rdh.backend.dto.response.ApiResponse;
import com.rdh.backend.dto.response.TextStegoResponse;
import com.rdh.backend.service.TextStegoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * REST API Controller for Text Steganography (File Upload Only).
 * 
 * Provides file upload endpoints for hiding secret data in text files using Unicode zero-width characters.
 * Works with any text format: plain text, Markdown, JSON, XML, HTML, source code, CSV, etc.
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-09
 */
@RestController
@RequestMapping("/api/v1/text-stego")
@Validated
@Tag(name = "Giấu Tin trong Văn Bản (Text Steganography)", description = "Nhúng dữ liệu bí mật vào file văn bản bằng ký tự Unicode vô hình")
public class TextStegoController {
    
    private final TextStegoService textStegoService;
    
    public TextStegoController(TextStegoService textStegoService) {
        this.textStegoService = textStegoService;
    }
    
    /**
     * Get information about text steganography algorithms.
     */
    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Lấy thông tin về Text Steganography",
        description = "Trả về thông tin về thuật toán, định dạng file hỗ trợ và hướng dẫn sử dụng."
    )
    public ResponseEntity<ApiResponse<Object>> getInfo() {
        return ResponseEntity.ok(ApiResponse.success("""
            {
              "algorithm": "Unicode Zero-Width Characters (Ký tự Unicode vô hình)",
              "description": "Giấu dữ liệu bí mật bằng ký tự Unicode vô hình (U+200B, U+200D, U+200C)",
              "supportedFormats": [
                "Plain Text (.txt)",
                "Markdown (.md)",
                "JSON (.json)",
                "XML (.xml)",
                "HTML (.html)",
                "Source Code (.java, .py, .js, .cpp, v.v.)",
                "CSV (.csv)",
                "Configuration files (.properties, .yml, .env)",
                "Mọi file văn bản UTF-8"
              ],
              "features": [
                "Vô hình với mắt thường",
                "Hoạt động với mọi định dạng văn bản",
                "Dung lượng tốt (~1 byte per 8 ký tự)",
                "Khó phát hiện nếu không phân tích",
                "Giữ nguyên định dạng văn bản"
              ],
              "usage": {
                "embed": "POST /api/v1/text-stego/embed-file (Upload file + secret → Tải file stego)",
                "extract": "POST /api/v1/text-stego/extract-file (Upload file stego → Xem secret)",
                "detect": "POST /api/v1/text-stego/detect-file (Upload file → Kiểm tra có hidden data)",
                "capacity": "POST /api/v1/text-stego/capacity-file (Upload file → Tính dung lượng)",
                "clean": "POST /api/v1/text-stego/clean-file (Upload file stego → Tải file đã xóa hidden data)"
              }
            }
            """));
    }
    
    /**
     * Embed secret data into text file.
     */
    @PostMapping(value = "/embed-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(
        summary = "Nhúng dữ liệu bí mật vào file văn bản",
        description = "Upload file văn bản (txt, md, json, code, v.v.) và nhúng dữ liệu bí mật. " +
                     "Tải xuống file stego. Hỗ trợ MỌI định dạng file văn bản UTF-8."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "File stego được tạo thành công, sẵn sàng tải xuống",
            content = @Content(
                mediaType = "application/octet-stream"
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "File không hợp lệ hoặc dữ liệu bí mật quá dài so với dung lượng file"
        )
    })
    public ResponseEntity<Resource> embedFile(
            @Parameter(description = "File văn bản để giấu dữ liệu (txt, md, json, code, v.v.)", required = true)
            @RequestPart("file") MultipartFile file,
            
            @Parameter(description = "Dữ liệu bí mật cần giấu", required = true)
            @RequestPart("secretData") String secretData) throws IOException {
        
        // Read file content
        String coverText = new String(file.getBytes(), StandardCharsets.UTF_8);
        
        // Create request
        TextStegoRequest request = new TextStegoRequest();
        request.setCoverText(coverText);
        request.setSecretData(secretData);
        
        // Embed
        TextStegoResponse response = textStegoService.embed(request);
        String stegoText = response.getStegoText();
        
        // Prepare file for download
        byte[] stegoBytes = stegoText.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(stegoBytes);
        
        // Generate filename: original_name_stego.ext
        String originalFilename = file.getOriginalFilename();
        String stegoFilename;
        if (originalFilename != null && originalFilename.contains(".")) {
            int dotIndex = originalFilename.lastIndexOf('.');
            String name = originalFilename.substring(0, dotIndex);
            String ext = originalFilename.substring(dotIndex);
            stegoFilename = name + "_stego" + ext;
        } else {
            stegoFilename = (originalFilename != null ? originalFilename : "stego") + "_stego.txt";
        }
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + stegoFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(stegoBytes.length)
                .body(resource);
    }
    
    /**
     * Extract secret data from stego file.
     */
    @PostMapping(value = "/extract-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Trích xuất dữ liệu bí mật từ file văn bản stego",
        description = "Upload file văn bản stego và trích xuất dữ liệu bí mật ẩn. " +
                     "Trả về thông điệp bí mật đã được trích xuất dưới dạng JSON."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Trích xuất dữ liệu bí mật thành công",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "code": 200,
                      "message": "Thao tác thành công",
                      "data": {
                        "success": true,
                        "message": "Secret data extracted successfully",
                        "secretData": "My secret message",
                        "metadata": {
                          "fileName": "example_stego.txt",
                          "fileSize": 1234,
                          "extractedLength": 17
                        }
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Không tìm thấy dữ liệu ẩn trong file"
        )
    })
    public ResponseEntity<ApiResponse<TextStegoResponse>> extractFile(
            @Parameter(description = "File văn bản stego chứa dữ liệu ẩn", required = true)
            @RequestPart("file") MultipartFile file) throws IOException {
        
        // Read file content
        String stegoText = new String(file.getBytes(), StandardCharsets.UTF_8);
        
        // Extract
        TextStegoResponse response = textStegoService.extract(stegoText);
        
        // Add file metadata
        response.getMetadata().put("fileName", file.getOriginalFilename());
        response.getMetadata().put("fileSize", file.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Detect if file contains hidden data.
     */
    @PostMapping(value = "/detect-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Phát hiện dữ liệu ẩn trong file văn bản",
        description = "Upload file văn bản để kiểm tra xem có chứa dữ liệu ẩn hay không. " +
                     "Trả về kết quả phát hiện và thống kê chi tiết."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Phát hiện hoàn tất",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "code": 200,
                      "message": "Thao tác thành công",
                      "data": {
                        "success": true,
                        "message": "Hidden data detected",
                        "metadata": {
                          "fileName": "example.txt",
                          "fileSize": 1234,
                          "hasHiddenData": true,
                          "statistics": "ZW Characters: 48 total (ZWSP: 24, ZWJ: 24)..."
                        }
                      }
                    }
                    """)
            )
        )
    })
    public ResponseEntity<ApiResponse<TextStegoResponse>> detectFile(
            @Parameter(description = "File văn bản cần phân tích", required = true)
            @RequestPart("file") MultipartFile file) throws IOException {
        
        // Read file content
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        
        // Detect
        TextStegoResponse response = textStegoService.detect(text);
        
        // Add file metadata
        response.getMetadata().put("fileName", file.getOriginalFilename());
        response.getMetadata().put("fileSize", file.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Calculate capacity of text file.
     */
    @PostMapping(value = "/capacity-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Tính dung lượng nhúng của file văn bản",
        description = "Upload file văn bản để tính toán có thể giấu bao nhiêu byte dữ liệu bí mật. " +
                     "Trả về thông tin dung lượng và đề xuất sử dụng."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tính toán dung lượng thành công",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = """
                    {
                      "code": 200,
                      "message": "Thao tác thành công",
                      "data": {
                        "success": true,
                        "message": "Capacity calculated successfully",
                        "metadata": {
                          "fileName": "example.txt",
                          "fileSize": 5000,
                          "capacityBytes": 150,
                          "capacityBits": 1200,
                          "maxSecretLength": 150,
                          "recommendation": "High capacity. Excellent for larger secret data."
                        }
                      }
                    }
                    """)
            )
        )
    })
    public ResponseEntity<ApiResponse<TextStegoResponse>> calculateCapacityFile(
            @Parameter(description = "File văn bản cần phân tích", required = true)
            @RequestPart("file") MultipartFile file) throws IOException {
        
        // Read file content
        String coverText = new String(file.getBytes(), StandardCharsets.UTF_8);
        
        // Calculate capacity
        TextStegoResponse response = textStegoService.calculateCapacity(coverText);
        
        // Add file metadata
        response.getMetadata().put("fileName", file.getOriginalFilename());
        response.getMetadata().put("fileSize", file.getSize());
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * Clean hidden data from text file.
     */
    @PostMapping(value = "/clean-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(
        summary = "Xóa dữ liệu ẩn khỏi file văn bản",
        description = "Upload file văn bản stego và loại bỏ tất cả dữ liệu ẩn. " +
                     "Tải xuống file đã được làm sạch."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "File đã được làm sạch thành công, sẵn sàng tải xuống",
            content = @Content(
                mediaType = "application/octet-stream"
            )
        )
    })
    public ResponseEntity<Resource> cleanFile(
            @Parameter(description = "File văn bản stego cần làm sạch", required = true)
            @RequestPart("file") MultipartFile file) throws IOException {
        
        // Read file content
        String stegoText = new String(file.getBytes(), StandardCharsets.UTF_8);
        
        // Clean
        TextStegoResponse response = textStegoService.clean(stegoText);
        String cleanText = response.getStegoText();
        
        // Prepare file for download
        byte[] cleanBytes = cleanText.getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(cleanBytes);
        
        // Generate filename: original_name_clean.ext
        String originalFilename = file.getOriginalFilename();
        String cleanFilename;
        if (originalFilename != null && originalFilename.contains(".")) {
            int dotIndex = originalFilename.lastIndexOf('.');
            String name = originalFilename.substring(0, dotIndex);
            String ext = originalFilename.substring(dotIndex);
            cleanFilename = name + "_clean" + ext;
        } else {
            cleanFilename = (originalFilename != null ? originalFilename : "clean") + "_clean.txt";
        }
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cleanFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(cleanBytes.length)
                .body(resource);
    }
}
