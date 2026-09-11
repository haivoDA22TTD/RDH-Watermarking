package com.rdh.backend.controller;

import com.rdh.backend.dto.response.ApiResponse;
import com.rdh.backend.dto.response.ExtractResponse;
import com.rdh.backend.service.RdhService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rdh")
@RequiredArgsConstructor
public class RdhController {

    private final RdhService rdhService;

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
}