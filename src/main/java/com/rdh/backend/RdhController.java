package com.rdh.backend;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/rdh")
@RequiredArgsConstructor // Lombok tự sinh Constructor tiêm RdhService
public class RdhController {

    private final RdhService rdhService;

    @PostMapping(value = "/embed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> embedData(
            @RequestPart("image") MultipartFile image,
            @RequestParam("secretData") String secretData) throws IOException {

        // Gọi logic xử lý từ Service
        byte[] stegoImageBytes = rdhService.embed(image, secretData);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"stego_image.png\"")
                .body(stegoImageBytes);
    }

    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> extractData(
            @RequestPart("image") MultipartFile stegoImage) throws IOException {

        // Gọi logic xử lý từ Service
        Map<String, String> result = rdhService.extractAndRestore(stegoImage);

        return ResponseEntity.ok(result);
    }
}