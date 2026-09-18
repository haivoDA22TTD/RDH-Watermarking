package com.rdh.backend.controller;

import com.rdh.backend.dto.response.ApiResponse;
import com.rdh.backend.dto.response.BatchEmbedResponse;
import com.rdh.backend.dto.response.BatchExtractResponse;
import com.rdh.backend.dto.response.EvaluationResponse;
import com.rdh.backend.dto.response.ExtractResponse;
import com.rdh.backend.service.RdhService;
import com.rdh.backend.validation.ImageFileValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rdh")
@Validated
@Tag(name = "🔐 Single Processing", description = "API xử lý từng ảnh riêng lẻ")
public class RdhController {

    private final RdhService rdhService;
    private final ImageFileValidator imageFileValidator;

    public RdhController(RdhService rdhService, ImageFileValidator imageFileValidator) {
        this.rdhService = rdhService;
        this.imageFileValidator = imageFileValidator;
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║  RdhController initialized with VALIDATE  ║");
        System.out.println("╚═══════════════════════════════════════════╝");
    }

    @PostConstruct
    public void logEndpoints() {
        System.out.println("📍 Available endpoints:");
        System.out.println("  - GET  /api/v1/rdh/algorithms");
        System.out.println("  - POST /api/v1/rdh/embed");
        System.out.println("  - POST /api/v1/rdh/extract");
        System.out.println("  - POST /api/v1/rdh/evaluate ✨ NEW");
        System.out.println("  - POST /api/v1/rdh/embed-batch 📦 BATCH");
        System.out.println("  - POST /api/v1/rdh/extract-batch 📦 BATCH");
    }

    // 1. GET danh sách thuật toán
    @Operation(
            summary = "📋 Lấy danh sách thuật toán RDH có sẵn",
            description = """
                    Trả về danh sách các thuật toán Reversible Data Hiding được hỗ trợ bởi hệ thống.
                    
                    **Hiện tại hỗ trợ:**
                    - **Histogram Shifting**: Thuật toán RDH cổ điển, hiệu quả cao
                    """,
            tags = {"📋 System"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Thành công - Trả về danh sách thuật toán",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/algorithms")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAlgorithms() {
        return ResponseEntity.ok(ApiResponse.success(rdhService.getAvailableAlgorithms()));
    }

    // 2. POST nhúng tin ẩn (Embed)
    @Operation(
            summary = "🔐 Nhúng dữ liệu bí mật vào ảnh",
            description = """
                    Nhúng một chuỗi text bí mật vào ảnh sử dụng thuật toán Histogram Shifting.
                    
                    **Input:**
                    - Ảnh gốc (PNG, JPG, BMP)
                    - Dữ liệu cần nhúng (text)
                    
                    **Output:**
                    - File ảnh stego (PNG format)
                    - Chất lượng: PSNR > 50 dB
                    - Có thể khôi phục 100% ảnh gốc sau khi extract
                    
                    **Lưu ý:**
                    - Dung lượng dữ liệu phụ thuộc vào capacity của ảnh
                    - Sử dụng `/evaluate` để kiểm tra capacity trước khi embed
                    """,
            tags = {"🔐 Single Processing"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Nhúng thành công - Trả về file ảnh stego",
                    content = @Content(
                            mediaType = "image/png",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "❌ Lỗi - Ảnh không hợp lệ hoặc dữ liệu quá lớn"
            )
    })
    @PostMapping(value = "/embed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> embedData(
            @Parameter(description = "File ảnh gốc (PNG/JPG/BMP)", required = true)
            @RequestPart("image") @NotNull MultipartFile image,
            
            @Parameter(description = "Dữ liệu bí mật cần nhúng (text)", required = true, example = "My secret message")
            @RequestParam("secretData") @NotBlank String secretData) throws IOException {

        // Validate image file
        imageFileValidator.validateImageFile(image);
        
        byte[] stegoImageBytes = rdhService.embed(image, secretData);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stego_image.png\"")
                .body(stegoImageBytes);
    }

    // 3. POST giải mã tin ẩn (Extract)
    @Operation(
            summary = "🔓 Giải mã dữ liệu từ ảnh stego",
            description = """
                    Trích xuất dữ liệu bí mật đã được nhúng trong ảnh stego.
                    
                    **Input:**
                    - Ảnh stego (từ endpoint `/embed`)
                    
                    **Output:**
                    - Dữ liệu text đã giải mã
                    - Nếu không tìm thấy: "Không tìm thấy tin ẩn"
                    
                    **Lưu ý:**
                    - Chỉ hoạt động với ảnh từ hệ thống này
                    - Ảnh stego phải là file PNG nguyên bản (không được edit/resize)
                    """,
            tags = {"🔐 Single Processing"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Giải mã thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "Thao tác thành công",
                                      "data": {
                                        "extractedData": "My secret message"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "❌ Lỗi - File không hợp lệ"
            )
    })
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExtractResponse>> extractData(
            @Parameter(description = "File ảnh stego (PNG)", required = true)
            @RequestPart("image") @NotNull MultipartFile stegoImage) throws IOException {

        // Validate image file
        imageFileValidator.validateImageFile(stegoImage);
        
        ExtractResponse response = rdhService.extractAndRestore(stegoImage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 4. POST đánh giá chất lượng (Evaluate)
    @Operation(
            summary = "📊 Đánh giá chất lượng ảnh stego",
            description = """
                    So sánh ảnh gốc và ảnh stego để tính toán các metrics chất lượng.
                    
                    **Metrics:**
                    - **PSNR** (Peak Signal-to-Noise Ratio): Đánh giá độ nhiễu, đơn vị dB
                      - > 50 dB: Xuất sắc
                      - 40-50 dB: Rất tốt
                      - 30-40 dB: Tốt
                    - **SSIM** (Structural Similarity Index): Độ tương đồng cấu trúc, giá trị 0-1
                      - > 0.99: Gần như giống hệt
                      - 0.95-0.99: Tương đồng cao
                    - **Capacity**: Số bit tối đa có thể nhúng vào ảnh
                    
                    **Use cases:**
                    - Kiểm tra chất lượng sau khi embed
                    - So sánh hiệu quả các thuật toán
                    - Ước lượng capacity trước khi embed
                    """,
            tags = {"🔐 Single Processing"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Đánh giá thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "Thao tác thành công",
                                      "data": {
                                        "psnr": 57.88,
                                        "capacity": 5396,
                                        "ssim": 0.9999,
                                        "width": 450,
                                        "height": 450,
                                        "message": "Đánh giá chất lượng thành công"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping(value = "/evaluate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluateQuality(
            @Parameter(description = "Ảnh gốc (chưa nhúng tin)", required = true)
            @RequestPart(value = "originalImage", required = true) @NotNull MultipartFile originalImage,
            
            @Parameter(description = "Ảnh stego (đã nhúng tin)", required = true)
            @RequestPart(value = "stegoImage", required = true) @NotNull MultipartFile stegoImage) throws IOException {

        System.out.println("=== EVALUATE ENDPOINT CALLED ===");
        System.out.println("Original image: " + originalImage.getOriginalFilename() + ", size: " + originalImage.getSize());
        System.out.println("Stego image: " + stegoImage.getOriginalFilename() + ", size: " + stegoImage.getSize());

        // Validate image files
        imageFileValidator.validateImageFile(originalImage);
        imageFileValidator.validateImageFile(stegoImage);
        
        EvaluationResponse response = rdhService.evaluateImageQuality(originalImage, stegoImage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Test endpoint để verify routing
    @GetMapping("/evaluate/test")
    public ResponseEntity<String> testEvaluate() {
        return ResponseEntity.ok("Evaluate endpoint is working!");
    }

    // 5. POST nhúng tin hàng loạt (Batch Embed)
    @Operation(
            summary = "📦 Nhúng dữ liệu vào nhiều ảnh cùng lúc",
            description = """
                    Nhúng cùng một chuỗi text vào nhiều ảnh, trả về file ZIP chứa tất cả ảnh stego.
                    
                    **Input:**
                    - Danh sách ảnh (có thể chọn nhiều file)
                    - Dữ liệu cần nhúng (áp dụng cho tất cả ảnh)
                    
                    **Output:**
                    - File ZIP chứa các file `stego_*.png`
                    - Headers: X-Total-Images, X-Success-Count, X-Failure-Count
                    
                    **Batch Processing:**
                    - Xử lý tuần tự từng ảnh
                    - Partial success: một số ảnh lỗi không làm fail toàn bộ
                    - Giới hạn: 200MB/request
                    
                    **Demo:**
                    - Khuyến nghị: 3-5 ảnh, mỗi ảnh < 5MB
                    """,
            tags = {"📦 Batch Processing"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Batch embed thành công - Trả về file ZIP",
                    content = @Content(
                            mediaType = "application/octet-stream",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "413",
                    description = "❌ Request quá lớn (> 200MB)"
            )
    })
    @PostMapping(value = "/embed-batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> embedBatch(
            @Parameter(description = "Danh sách ảnh gốc (chọn nhiều file)", required = true)
            @RequestPart(value = "images", required = true) @NotNull List<MultipartFile> images,
            
            @Parameter(description = "Dữ liệu bí mật (áp dụng cho tất cả ảnh)", required = true, example = "Batch secret")
            @RequestParam("secretData") @NotBlank String secretData) throws IOException {

        System.out.println("=== BATCH EMBED ENDPOINT CALLED ===");
        System.out.println("Number of images: " + images.size());
        System.out.println("Secret data: " + secretData);

        // Validate image files (batch)
        imageFileValidator.validateImageFiles(images);
        
        // Response holder để nhận metadata
        BatchEmbedResponse response = new BatchEmbedResponse();
        
        // Tạo ZIP file
        byte[] zipBytes = rdhService.embedBatch(images, secretData, response);

        // Log summary
        System.out.println("Batch embed result: " + response.getMessage());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stego_images.zip\"")
                .header("X-Total-Images", String.valueOf(response.getTotalImages()))
                .header("X-Success-Count", String.valueOf(response.getSuccessCount()))
                .header("X-Failure-Count", String.valueOf(response.getFailureCount()))
                .body(zipBytes);
    }

    // 6. POST giải mã hàng loạt (Batch Extract)
    @Operation(
            summary = "📦 Giải mã nhiều ảnh stego cùng lúc",
            description = """
                    Trích xuất dữ liệu từ nhiều ảnh stego, trả về JSON array với kết quả từng ảnh.
                    
                    **Input:**
                    - Danh sách ảnh stego (từ ZIP của `/embed-batch`)
                    
                    **Output:**
                    - JSON object chứa:
                      - `totalImages`: Tổng số ảnh
                      - `successCount`: Số ảnh extract thành công
                      - `failureCount`: Số ảnh extract thất bại
                      - `results`: Array kết quả chi tiết từng ảnh
                    
                    **Partial Success:**
                    - Từng ảnh có status riêng: SUCCESS hoặc FAILED
                    - Ảnh lỗi có `errorMessage` giải thích
                    
                    **Use cases:**
                    - Kiểm tra batch embed có thành công không
                    - Extract metadata từ nhiều ảnh forensics
                    """,
            tags = {"📦 Batch Processing"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Batch extract thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "Thao tác thành công",
                                      "data": {
                                        "totalImages": 3,
                                        "successCount": 3,
                                        "failureCount": 0,
                                        "message": "Extracted 3 images: 3 success, 0 failed",
                                        "results": [
                                          {
                                            "fileName": "stego_image1.png",
                                            "extractedData": "Batch secret",
                                            "status": "SUCCESS",
                                            "errorMessage": null,
                                            "fileSize": 123456
                                          }
                                        ]
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping(value = "/extract-batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BatchExtractResponse>> extractBatch(
            @Parameter(description = "Danh sách ảnh stego (chọn nhiều file)", required = true)
            @RequestPart(value = "images", required = true) @NotNull List<MultipartFile> images) throws IOException {

        System.out.println("=== BATCH EXTRACT ENDPOINT CALLED ===");
        System.out.println("Number of images: " + images.size());

        // Validate image files (batch)
        imageFileValidator.validateImageFiles(images);
        
        BatchExtractResponse response = rdhService.extractBatch(images);

        // Log summary
        System.out.println("Batch extract result: " + response.getMessage());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}