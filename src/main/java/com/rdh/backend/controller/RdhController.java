package com.rdh.backend.controller;

import com.rdh.backend.dto.response.ApiResponse;
import com.rdh.backend.dto.response.EvaluationResponse;
import com.rdh.backend.dto.response.ExtractResponse;
import com.rdh.backend.service.RdhService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rdh")
public class RdhController {

    private final RdhService rdhService;

    public RdhController(RdhService rdhService) {
        this.rdhService = rdhService;
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║  RdhController initialized with EVALUATE  ║");
        System.out.println("╚═══════════════════════════════════════════╝");
    }

    @PostConstruct
    public void logEndpoints() {
        System.out.println("📍 Available endpoints:");
        System.out.println("  - GET  /api/v1/rdh/algorithms");
        System.out.println("  - POST /api/v1/rdh/embed");
        System.out.println("  - POST /api/v1/rdh/extract");
        System.out.println("  - POST /api/v1/rdh/evaluate ✨ NEW");
    }

    // 1. GET danh sách thuật toán
    @GetMapping("/algorithms")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getAlgorithms() {
        return ResponseEntity.ok(ApiResponse.success(rdhService.getAvailableAlgorithms()));
    }

    // 2. POST nhúng tin ẩn (Embed)
    @PostMapping(value = "/embed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> embedData(
            @RequestPart("image") MultipartFile image,
            @RequestParam("secretData") String secretData) throws IOException {

        byte[] stegoImageBytes = rdhService.embed(image, secretData);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stego_image.png\"")
                .body(stegoImageBytes);
    }

    // 3. POST giải mã tin ẩn (Extract)
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExtractResponse>> extractData(
            @RequestPart("image") MultipartFile stegoImage) throws IOException {

        ExtractResponse response = rdhService.extractAndRestore(stegoImage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 4. POST đánh giá chất lượng (Evaluate)
    @PostMapping(value = "/evaluate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<EvaluationResponse>> evaluateQuality(
            @RequestPart(value = "originalImage", required = true) MultipartFile originalImage,
            @RequestPart(value = "stegoImage", required = true) MultipartFile stegoImage) throws IOException {

        System.out.println("=== EVALUATE ENDPOINT CALLED ===");
        System.out.println("Original image: " + originalImage.getOriginalFilename() + ", size: " + originalImage.getSize());
        System.out.println("Stego image: " + stegoImage.getOriginalFilename() + ", size: " + stegoImage.getSize());

        EvaluationResponse response = rdhService.evaluateImageQuality(originalImage, stegoImage);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // Test endpoint để verify routing
    @GetMapping("/evaluate/test")
    public ResponseEntity<String> testEvaluate() {
        return ResponseEntity.ok("Evaluate endpoint is working!");
    }
}