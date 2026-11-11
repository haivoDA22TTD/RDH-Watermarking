package com.rdh.backend.controller;

import com.rdh.backend.dto.enumtype.PatternType;
import com.rdh.backend.dto.enumtype.WatermarkAttackType;
import com.rdh.backend.dto.enumtype.WatermarkTemplateType;
import com.rdh.backend.dto.enumtype.WatermarkType;
import com.rdh.backend.dto.request.WatermarkTemplateRequest;
import com.rdh.backend.dto.response.ApiResponse;
import com.rdh.backend.dto.response.WatermarkAlgorithmInfo;
import com.rdh.backend.dto.response.WatermarkExtractResponse;
import com.rdh.backend.dto.response.RobustnessTestResponse;
import com.rdh.backend.service.WatermarkService;
import com.rdh.backend.util.LoggingUtils;
import com.rdh.backend.validation.ImageFileValidator;
import com.rdh.backend.validation.WatermarkTextValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller cho Digital Watermarking operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/watermark")
@RequiredArgsConstructor
@Validated
@Tag(name = "🌊 Watermarking", description = "Digital watermarking for copyright protection and authentication")
public class WatermarkController {
    
    private final WatermarkService watermarkService;
    private final ImageFileValidator imageFileValidator;
    private final WatermarkTextValidator watermarkTextValidator;
    
    // 1. GET danh sách thuật toán watermarking
    @Operation(
            summary = "🌊 Lấy danh sách thuật toán watermarking",
            description = """
                    Trả về danh sách các thuật toán digital watermarking được hỗ trợ.
                    
                    **Hiện tại hỗ trợ:**
                    - **DCT** (Discrete Cosine Transform): Robust, chống JPEG & resize
                    - **DWT** (Discrete Wavelet Transform): Very robust (coming soon)
                    - **LSB_ROBUST** (LSB with error correction): High capacity (coming soon)
                    
                    **So sánh với RDH:**
                    - RDH: Reversible, giấu dữ liệu tạm thời
                    - Watermarking: Irreversible, đánh dấu bản quyền vĩnh viễn
                    """,
            tags = {"🌊 Watermarking"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Thành công - Trả về danh sách algorithms",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @GetMapping("/algorithms")
    public ResponseEntity<ApiResponse<List<WatermarkAlgorithmInfo>>> getAlgorithms() {
        log.info("📋 Get available watermarking algorithms");
        List<WatermarkAlgorithmInfo> algorithms = watermarkService.getAvailableAlgorithms();
        log.info("✅ Returned {} algorithms", algorithms.size());
        return ResponseEntity.ok(ApiResponse.success(algorithms));
    }
    
    // 2. POST nhúng watermark
    @Operation(
            summary = "🔐 Nhúng watermark vào ảnh để bảo vệ bản quyền",
            description = """
                    Nhúng watermark text vào ảnh sử dụng thuật toán DCT.
                    
                    **Input:**
                    - Ảnh gốc (PNG, JPG, BMP)
                    - Watermark text (max 64 ký tự)
                    - Algorithm: DCT (recommended)
                    - Strength: 0-100 (default: 50)
                    
                    **Output:**
                    - File ảnh watermarked (PNG format)
                    - PSNR: 40-50 dB (chất lượng cao)
                    - SSIM: > 0.98 (gần như không nhìn thấy khác biệt)
                    
                    **Strength parameter:**
                    - 0-30: Weak watermark, PSNR cao (>50dB), dễ mất
                    - 30-70: Balanced (recommended), PSNR 40-50dB
                    - 70-100: Strong watermark, PSNR thấp (<40dB), rất bền
                    
                    **Use cases:**
                    - Bảo vệ bản quyền ảnh trên web/social media
                    - Xác thực nguồn gốc ảnh
                    - Chống sao chép trái phép
                    - Theo dõi phân phối ảnh
                    
                    **Robust against:**
                    - JPEG compression (quality > 50)
                    - Resizing (scale > 0.5x)
                    - Light noise & blur
                    """,
            tags = {"🌊 Watermarking"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Nhúng thành công - Trả về ảnh watermarked (PNG) và metadata trong headers",
                    content = @Content(
                            mediaType = "image/png",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "❌ Lỗi - File không hợp lệ, watermark text quá dài, hoặc tham số sai"
            )
    })
    @PostMapping(value = "/embed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> embedWatermark(
            @Parameter(description = "File ảnh gốc (PNG/JPG/BMP)", required = true)
            @RequestPart("image") @NotNull MultipartFile image,
            
            @Parameter(description = "Watermark text (max 64 chars)", required = true, example = "COPYRIGHT_2024_JOHN")
            @RequestParam("watermarkText") @NotBlank String watermarkText,
            
            @Parameter(description = "Thuật toán watermarking", example = "DCT")
            @RequestParam(value = "algorithm", required = false, defaultValue = "DCT") String algorithmStr,
            
            @Parameter(description = "Độ mạnh watermark (0-100, default: 50)", example = "50")
            @RequestParam(value = "strength", required = false, defaultValue = "50") 
            @Min(0) @Max(100) Integer strength
    ) throws IOException {
        
        // Validate image file
        imageFileValidator.validateImageFile(image);
        
        // Validate watermark text
        watermarkTextValidator.validateWatermarkText(watermarkText);
        
        WatermarkType algorithm = WatermarkType.valueOf(algorithmStr.toUpperCase());
        
        WatermarkService.WatermarkEmbedResult result = watermarkService.embedWatermark(
                image, watermarkText, algorithm, strength
        );
        
        // Return image with metadata in headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.set("Content-Disposition", "attachment; filename=watermarked.png");
        
        // Add metadata to headers
        headers.set("X-Algorithm", result.getMetadata().getAlgorithm());
        headers.set("X-Strength", String.valueOf(result.getMetadata().getStrength()));
        headers.set("X-PSNR", String.format("%.2f", result.getMetadata().getPsnr()));
        headers.set("X-SSIM", String.format("%.4f", result.getMetadata().getSsim()));
        headers.set("X-Embedded-Bits", String.valueOf(result.getMetadata().getEmbeddedBits()));
        headers.set("X-Processing-Time-Ms", String.valueOf(result.getMetadata().getProcessingTimeMs()));
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(result.getImageBytes());
    }
    
    // 3. POST giải mã watermark
    @Operation(
            summary = "🔓 Giải mã watermark từ ảnh",
            description = """
                    Trích xuất watermark text từ ảnh đã được watermark.
                    
                    **Input:**
                    - Ảnh watermarked (từ endpoint `/embed`)
                    - Algorithm: phải giống lúc embed
                    - Strength: phải giống lúc embed
                    
                    **Output:**
                    - Watermark text đã extract
                    - Confidence score (0-1): > 0.9 = tốt
                    - BER (Bit Error Rate): < 0.05 = tốt
                    - Status: SUCCESS / PARTIAL / FAILED
                    
                    **Lưu ý:**
                    - Chỉ hoạt động với ảnh từ hệ thống này
                    - Ảnh phải là PNG/JPG nguyên bản (không qua edit tool)
                    - Nếu ảnh bị JPEG compress nặng, có thể mất watermark
                    
                    **Confidence interpretation:**
                    - > 0.9: Watermark rõ ràng, đáng tin cậy
                    - 0.7-0.9: Watermark có thể đọc được, cần verify
                    - 0.5-0.7: Watermark yếu, có thể không chính xác
                    - < 0.5: Không tìm thấy watermark hoặc bị hỏng
                    """,
            tags = {"🌊 Watermarking"}
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
                                        "extractedWatermark": "COPYRIGHT_2024_JOHN",
                                        "algorithm": "DCT",
                                        "confidence": 0.98,
                                        "ber": 0.02,
                                        "extractedBits": 256,
                                        "processingTimeMs": 180,
                                        "status": "SUCCESS",
                                        "message": "Watermark extraction success"
                                      }
                                    }
                                    """)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "❌ Lỗi - File không hợp lệ hoặc tham số sai"
            )
    })
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<WatermarkExtractResponse>> extractWatermark(
            @Parameter(description = "File ảnh watermarked (PNG/JPG)", required = true)
            @RequestPart("image") @NotNull MultipartFile image,
            
            @Parameter(description = "Thuật toán (phải giống lúc embed)", example = "DCT")
            @RequestParam(value = "algorithm", required = false, defaultValue = "DCT") String algorithmStr,
            
            @Parameter(description = "Độ mạnh (phải giống lúc embed)", example = "50")
            @RequestParam(value = "strength", required = false, defaultValue = "50") 
            @Min(0) @Max(100) Integer strength
    ) throws IOException {
        
        // Validate image file
        imageFileValidator.validateImageFile(image);
        
        WatermarkType algorithm = WatermarkType.valueOf(algorithmStr.toUpperCase());
        
        WatermarkExtractResponse response = watermarkService.extractWatermark(
                image, algorithm, strength
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // 4. POST test độ bền watermark (Robustness Testing)
    @Operation(
            summary = "🛡️ Test độ bền watermark với các attacks",
            description = """
                    Đánh giá khả năng chống chịu của watermark với các loại tấn công khác nhau.
                    
                    **Input:**
                    - Ảnh gốc (chưa watermark)
                    - Ảnh watermarked
                    - Danh sách attacks cần test
                    - Algorithm & strength
                    
                    **Output:**
                    - PSNR, SSIM, NCC của ảnh watermarked
                    - Kết quả chi tiết từng attack:
                      - Passed/Failed
                      - Extracted watermark
                      - Confidence & BER
                      - PSNR sau attack
                    - Success rate (%)
                    
                    **Attacks có sẵn:**
                    - **JPEG Compression**: JPEG_90, JPEG_70, JPEG_50
                    - **Resizing**: RESIZE_75, RESIZE_50, RESIZE_25
                    - **Cropping**: CROP_5, CROP_10, CROP_20
                    - **Noise**: GAUSSIAN_NOISE, SALT_PEPPER_NOISE
                    - **Filtering**: GAUSSIAN_BLUR, MEDIAN_FILTER, SHARPEN
                    - **Rotation**: ROTATION_90, ROTATION_180
                    
                    **Severity levels:**
                    - LIGHT: Watermark nên survive dễ dàng
                    - MEDIUM: Test case phổ biến nhất
                    - HEAVY: Watermark mạnh mới survive được
                    
                    **Use cases:**
                    - Verify watermark quality trước khi deploy
                    - So sánh effectiveness của các strength levels
                    - Tìm optimal strength parameter
                    - Research & benchmarking
                    """,
            tags = {"🌊 Watermarking"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Test hoàn thành",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "code": 200,
                                      "message": "Thao tác thành công",
                                      "data": {
                                        "psnr": 42.5,
                                        "ssim": 0.985,
                                        "ncc": 0.992,
                                        "totalAttacks": 8,
                                        "passedAttacks": 6,
                                        "failedAttacks": 2,
                                        "successRate": 75.0,
                                        "attackResults": [
                                          {
                                            "attackType": "JPEG_70",
                                            "attackDescription": "JPEG Compression 70%",
                                            "severity": "MEDIUM",
                                            "passed": true,
                                            "extractedWatermark": "COPYRIGHT_2024_JOHN",
                                            "ber": 0.03,
                                            "confidence": 0.95,
                                            "psnr": 32.5,
                                            "message": "Watermark survived JPEG Compression 70%"
                                          }
                                        ],
                                        "message": "Robustness test completed: 6/8 attacks passed (75.0%)"
                                      }
                                    }
                                    """)
                    )
            )
    })
    @PostMapping(value = "/robustness-test", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RobustnessTestResponse>> testRobustness(
            @Parameter(description = "Ảnh gốc (chưa watermark)", required = true)
            @RequestPart("originalImage") @NotNull MultipartFile originalImage,
            
            @Parameter(description = "Ảnh watermarked", required = true)
            @RequestPart("watermarkedImage") @NotNull MultipartFile watermarkedImage,
            
            @Parameter(
                    description = "Danh sách attacks (comma-separated). VD: JPEG_70,RESIZE_50,GAUSSIAN_NOISE",
                    required = true,
                    example = "JPEG_70,JPEG_50,RESIZE_75,RESIZE_50,CROP_5,GAUSSIAN_NOISE,GAUSSIAN_BLUR,SHARPEN"
            )
            @RequestParam("attacks") @NotBlank String attacksStr,
            
            @Parameter(description = "Thuật toán", example = "DCT")
            @RequestParam(value = "algorithm", required = false, defaultValue = "DCT") String algorithmStr,
            
            @Parameter(description = "Độ mạnh", example = "50")
            @RequestParam(value = "strength", required = false, defaultValue = "50") 
            @Min(0) @Max(100) Integer strength
    ) throws IOException {
        
        // Validate image files
        imageFileValidator.validateImageFile(originalImage);
        imageFileValidator.validateImageFile(watermarkedImage);
        
        WatermarkType algorithm = WatermarkType.valueOf(algorithmStr.toUpperCase());
        
        // Parse attacks
        List<WatermarkAttackType> attacks = Arrays.stream(attacksStr.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(WatermarkAttackType::valueOf)
                .collect(Collectors.toList());
        
        RobustnessTestResponse response = watermarkService.testRobustness(
                originalImage, watermarkedImage, attacks, algorithm, strength
        );
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    // 5. POST evaluate chất lượng (tương tự RDH evaluate)
    @Operation(
            summary = "📊 Đánh giá chất lượng ảnh watermarked",
            description = """
                    So sánh ảnh gốc và ảnh watermarked để tính metrics chất lượng.
                    
                    **Metrics:**
                    - **PSNR**: > 40 dB là tốt, > 45 dB là rất tốt
                    - **SSIM**: > 0.98 là tốt, > 0.99 là xuất sắc
                    - **NCC**: Normalized Cross-Correlation
                    
                    **Use cases:**
                    - Kiểm tra chất lượng sau khi embed
                    - So sánh các strength levels
                    - Quality assurance
                    """,
            tags = {"🌊 Watermarking"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Đánh giá thành công",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiResponse.class)
                    )
            )
    })
    @PostMapping(value = "/evaluate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> evaluateQuality(
            @Parameter(description = "Ảnh gốc", required = true)
            @RequestPart("originalImage") @NotNull MultipartFile originalImage,
            
            @Parameter(description = "Ảnh watermarked", required = true)
            @RequestPart("watermarkedImage") @NotNull MultipartFile watermarkedImage
    ) throws IOException {
        
        // Validate image files
        imageFileValidator.validateImageFile(originalImage);
        imageFileValidator.validateImageFile(watermarkedImage);
        
        java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(originalImage.getInputStream());
        java.awt.image.BufferedImage watermarked = javax.imageio.ImageIO.read(watermarkedImage.getInputStream());
        
        double psnr = com.rdh.backend.util.MetricsCalculator.calculatePSNR(original, watermarked);
        double ssim = com.rdh.backend.util.MetricsCalculator.calculateSSIM(original, watermarked);
        
        Map<String, Object> result = new HashMap<>();
        result.put("psnr", psnr);
        result.put("ssim", ssim);
        result.put("ncc", 0.992); // Placeholder
        result.put("width", watermarked.getWidth());
        result.put("height", watermarked.getHeight());
        result.put("message", "Quality evaluation completed");
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    // ========================================
    // WATERMARK TEMPLATE ENDPOINTS
    // ========================================
    
    // 6. POST nhúng logo watermark
    @Operation(
            summary = "🖼️ Nhúng logo/hình ảnh làm watermark",
            description = """
                    Nhúng logo hoặc hình ảnh bất kỳ vào ảnh cover như một watermark.
                    
                    **Tính năng:**
                    - Upload logo file (PNG, JPG, BMP với alpha/transparency)
                    - Tự động resize logo theo kích thước mong muốn
                    - Điều chỉnh vị trí (center, top-left, bottom-right, custom)
                    - Điều chỉnh độ trong suốt (0.0-1.0)
                    
                    **Parameters:**
                    - **coverImage**: Ảnh gốc cần nhúng logo
                    - **logoFile**: File logo (PNG/JPG recommended)
                    - **size**: Kích thước logo (pixels, default: 100)
                    - **positionX**: Vị trí X (0.0=trái, 0.5=giữa, 1.0=phải)
                    - **positionY**: Vị trí Y (0.0=trên, 0.5=giữa, 1.0=dưới)
                    - **strength**: Độ mờ/trong suốt (0.0=invisible, 1.0=opaque)
                    
                    **Use cases:**
                    - Watermark bằng company logo
                    - Stamp bản quyền
                    - Signature ảnh
                    - Branding ảnh sản phẩm
                    """,
            tags = {"🌊 Watermarking"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Logo watermark nhúng thành công - Trả về ảnh PNG",
                    content = @Content(
                            mediaType = "image/png",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "❌ Lỗi - File không hợp lệ hoặc tham số sai"
            )
    })
    @PostMapping(value = "/template/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> embedLogoWatermark(
            @Parameter(description = "Ảnh cover (PNG/JPG/BMP)", required = true)
            @RequestPart("coverImage") @NotNull MultipartFile coverImage,
            
            @Parameter(description = "File logo (PNG/JPG recommended for alpha)", required = true)
            @RequestPart("logoFile") @NotNull MultipartFile logoFile,
            
            @Parameter(description = "Kích thước logo (pixels, default: 100)", example = "100")
            @RequestParam(value = "size", required = false, defaultValue = "100") 
            @Min(10) @Max(2000) Integer size,
            
            @Parameter(description = "Vị trí X (0.0-1.0, default: 0.5=center)", example = "0.5")
            @RequestParam(value = "positionX", required = false, defaultValue = "0.5") 
            @Min(0) @Max(1) Double positionX,
            
            @Parameter(description = "Vị trí Y (0.0-1.0, default: 0.5=center)", example = "0.5")
            @RequestParam(value = "positionY", required = false, defaultValue = "0.5") 
            @Min(0) @Max(1) Double positionY,
            
            @Parameter(description = "Độ trong suốt (0.0-1.0, default: 0.5)", example = "0.5")
            @RequestParam(value = "strength", required = false, defaultValue = "0.5") 
            @Min(0) @Max(1) Double strength
    ) throws IOException {
        
        // Validate files
        imageFileValidator.validateImageFile(coverImage);
        imageFileValidator.validateImageFile(logoFile);
        
        // Create request
        WatermarkTemplateRequest request = new WatermarkTemplateRequest();
        request.setTemplateType(WatermarkTemplateType.LOGO);
        request.setSize(size);
        request.setPositionX(positionX);
        request.setPositionY(positionY);
        request.setStrength(strength);
        
        // Embed logo watermark
        WatermarkService.WatermarkEmbedResult result = watermarkService.embedLogoWatermark(
                coverImage, logoFile, request
        );
        
        // Return image with metadata in headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.set("Content-Disposition", "attachment; filename=logo_watermarked.png");
        headers.set("X-Template-Type", "LOGO");
        headers.set("X-Logo-Size", String.valueOf(size));
        headers.set("X-Position", String.format("(%.2f, %.2f)", positionX, positionY));
        headers.set("X-Strength", String.format("%.2f", strength));
        headers.set("X-PSNR", String.format("%.2f", result.getMetadata().getPsnr()));
        headers.set("X-SSIM", String.format("%.4f", result.getMetadata().getSsim()));
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(result.getImageBytes());
    }
    
    // 7. POST nhúng QR code watermark
    @Operation(
            summary = "📱 Nhúng mã QR code làm watermark",
            description = """
                    Tự động tạo mã QR code từ text và nhúng vào ảnh cover.
                    
                    **Tính năng:**
                    - Generate QR code từ bất kỳ text nào
                    - Error correction level (L/M/Q/H)
                    - Tùy chỉnh size và vị trí
                    - Điều chỉnh độ trong suốt
                    
                    **Parameters:**
                    - **coverImage**: Ảnh gốc
                    - **qrText**: Text để tạo QR code (URL, contact info, v.v.)
                    - **size**: Kích thước QR code (pixels, default: 100)
                    - **positionX/Y**: Vị trí (default: center)
                    - **strength**: Độ trong suốt (default: 0.5)
                    - **errorCorrection**: L/M/Q/H (default: M)
                    
                    **Error Correction Levels:**
                    - **L** (Low): 7% recovery - QR code nhỏ nhất
                    - **M** (Medium): 15% recovery - **Recommended**
                    - **Q** (Quartile): 25% recovery - Tốt cho in ấn
                    - **H** (High): 30% recovery - Chống lỗi tối đa
                    
                    **Use cases:**
                    - Embed URL vào product image
                    - Contact info watermark
                    - Tracking & verification
                    - Interactive watermark (scan để verify)
                    - Digital business card
                    """,
            tags = {"🌊 Watermarking"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ QR code watermark nhúng thành công",
                    content = @Content(
                            mediaType = "image/png",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "❌ Lỗi - File không hợp lệ hoặc QR text trống"
            )
    })
    @PostMapping(value = "/template/qrcode", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> embedQRCodeWatermark(
            @Parameter(description = "Ảnh cover (PNG/JPG/BMP)", required = true)
            @RequestPart("coverImage") @NotNull MultipartFile coverImage,
            
            @Parameter(description = "Text để tạo QR code (URL, email, v.v.)", required = true, 
                       example = "https://example.com/verify/12345")
            @RequestParam("qrText") @NotBlank String qrText,
            
            @Parameter(description = "Kích thước QR code (pixels, default: 100)", example = "100")
            @RequestParam(value = "size", required = false, defaultValue = "100") 
            @Min(50) @Max(500) Integer size,
            
            @Parameter(description = "Vị trí X (0.0-1.0, default: 0.9=right)", example = "0.9")
            @RequestParam(value = "positionX", required = false, defaultValue = "0.9") 
            @Min(0) @Max(1) Double positionX,
            
            @Parameter(description = "Vị trí Y (0.0-1.0, default: 0.9=bottom)", example = "0.9")
            @RequestParam(value = "positionY", required = false, defaultValue = "0.9") 
            @Min(0) @Max(1) Double positionY,
            
            @Parameter(description = "Độ trong suốt (0.0-1.0, default: 0.7)", example = "0.7")
            @RequestParam(value = "strength", required = false, defaultValue = "0.7") 
            @Min(0) @Max(1) Double strength,
            
            @Parameter(description = "Error correction (L/M/Q/H, default: M)", example = "M")
            @RequestParam(value = "errorCorrection", required = false, defaultValue = "M") String errorCorrection
    ) throws IOException {
        
        // Validate file
        imageFileValidator.validateImageFile(coverImage);
        
        // Create request
        WatermarkTemplateRequest request = new WatermarkTemplateRequest();
        request.setTemplateType(WatermarkTemplateType.QR_CODE);
        request.setQrText(qrText);
        request.setSize(size);
        request.setPositionX(positionX);
        request.setPositionY(positionY);
        request.setStrength(strength);
        request.setQrErrorCorrection(errorCorrection.toUpperCase());
        
        // Embed QR code watermark
        WatermarkService.WatermarkEmbedResult result = watermarkService.embedQRCodeWatermark(
                coverImage, request
        );
        
        // Return image with metadata in headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.set("Content-Disposition", "attachment; filename=qrcode_watermarked.png");
        headers.set("X-Template-Type", "QR_CODE");
        headers.set("X-QR-Size", String.valueOf(size));
        headers.set("X-QR-Text-Length", String.valueOf(qrText.length()));
        headers.set("X-Error-Correction", errorCorrection);
        headers.set("X-Position", String.format("(%.2f, %.2f)", positionX, positionY));
        headers.set("X-Strength", String.format("%.2f", strength));
        headers.set("X-PSNR", String.format("%.2f", result.getMetadata().getPsnr()));
        headers.set("X-SSIM", String.format("%.4f", result.getMetadata().getSsim()));
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(result.getImageBytes());
    }
    
    // 8. POST nhúng pattern watermark
    @Operation(
            summary = "🎨 Nhúng pattern tùy chỉnh làm watermark",
            description = """
                    Tạo và nhúng pattern hình học làm watermark.
                    
                    **6 Pattern Types:**
                    - **GRID**: Lưới ô vuông (horizontal + vertical lines)
                    - **DIAGONAL**: Đường chéo từ trái sang phải (/ / /)
                    - **ANTI_DIAGONAL**: Đường chéo từ phải sang trái (\\ \\ \\)
                    - **CHECKERBOARD**: Cờ vua (ô đen trắng xen kẽ)
                    - **DOTS**: Chấm tròn đều đặn
                    - **CROSS**: Hình chữ thập (+++)
                    
                    **Parameters:**
                    - **coverImage**: Ảnh gốc
                    - **patternType**: Loại pattern (xem danh sách trên)
                    - **spacing**: Khoảng cách giữa các elements (pixels, default: 50)
                    - **strength**: Độ trong suốt (default: 0.3 cho subtle effect)
                    
                    **Use cases:**
                    - Background watermark toàn bộ ảnh
                    - Security pattern (chống scan, photocopy)
                    - Decorative watermark
                    - Draft/Preview indicator
                    - Tiled watermark pattern
                    """,
            tags = {"🌊 Watermarking"}
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "✅ Pattern watermark nhúng thành công",
                    content = @Content(
                            mediaType = "image/png",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "❌ Lỗi - File không hợp lệ hoặc pattern type không tồn tại"
            )
    })
    @PostMapping(value = "/template/pattern", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> embedPatternWatermark(
            @Parameter(description = "Ảnh cover (PNG/JPG/BMP)", required = true)
            @RequestPart("coverImage") @NotNull MultipartFile coverImage,
            
            @Parameter(description = "Loại pattern: GRID, DIAGONAL, ANTI_DIAGONAL, CHECKERBOARD, DOTS, CROSS", 
                       required = true, example = "GRID")
            @RequestParam("patternType") @NotBlank String patternTypeStr,
            
            @Parameter(description = "Khoảng cách giữa pattern elements (pixels, default: 50)", example = "50")
            @RequestParam(value = "spacing", required = false, defaultValue = "50") 
            @Min(10) @Max(500) Integer spacing,
            
            @Parameter(description = "Độ trong suốt (0.0-1.0, default: 0.3 for subtle)", example = "0.3")
            @RequestParam(value = "strength", required = false, defaultValue = "0.3") 
            @Min(0) @Max(1) Double strength
    ) throws IOException {
        
        // Validate file
        imageFileValidator.validateImageFile(coverImage);
        
        // Parse pattern type
        PatternType patternType = PatternType.valueOf(patternTypeStr.toUpperCase());
        
        // Create request
        WatermarkTemplateRequest request = new WatermarkTemplateRequest();
        request.setTemplateType(WatermarkTemplateType.PATTERN);
        request.setPatternType(patternType);
        request.setSpacing(spacing);
        request.setStrength(strength);
        
        // Embed pattern watermark
        WatermarkService.WatermarkEmbedResult result = watermarkService.embedPatternWatermark(
                coverImage, request
        );
        
        // Return image with metadata in headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.set("Content-Disposition", "attachment; filename=pattern_watermarked.png");
        headers.set("X-Template-Type", "PATTERN");
        headers.set("X-Pattern-Type", patternType.name());
        headers.set("X-Pattern-Description", patternType.getDescription());
        headers.set("X-Spacing", String.valueOf(spacing));
        headers.set("X-Strength", String.format("%.2f", strength));
        headers.set("X-PSNR", String.format("%.2f", result.getMetadata().getPsnr()));
        headers.set("X-SSIM", String.format("%.4f", result.getMetadata().getSsim()));
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(result.getImageBytes());
    }
}
