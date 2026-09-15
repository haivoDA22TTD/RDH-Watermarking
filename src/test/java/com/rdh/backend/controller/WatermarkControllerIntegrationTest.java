package com.rdh.backend.controller;

import com.rdh.backend.util.TestDataFactory;
import com.rdh.backend.util.TestImageGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests cho WatermarkController
 * Test tất cả 5 endpoints với MockMvc
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("WatermarkController Integration Tests")
class WatermarkControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("GET /algorithms should return list of watermark algorithms")
    void testGetAlgorithms() throws Exception {
        mockMvc.perform(get("/api/v1/watermark/algorithms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data[0].name").value("DCT"))
                .andExpect(jsonPath("$.data[0].displayName").exists())
                .andExpect(jsonPath("$.data[0].robustness").exists());
    }
    
    @Test
    @DisplayName("POST /embed should embed watermark and return PNG image")
    void testEmbedWatermark() throws Exception {
        // Given
        MockMultipartFile imageFile = TestDataFactory.createDefaultTestImageFile();
        
        // When/Then
        mockMvc.perform(multipart("/api/v1/watermark/embed")
                        .file(imageFile)
                        .param("watermarkText", "TEST_COPYRIGHT_2024")
                        .param("algorithm", "DCT")
                        .param("strength", "50"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().exists("X-Algorithm"))
                .andExpect(header().string("X-Algorithm", "DCT"))
                .andExpect(header().exists("X-PSNR"))
                .andExpect(header().exists("X-SSIM"))
                .andExpect(header().exists("X-Embedded-Bits"));
    }
    
    @Test
    @DisplayName("POST /embed should fail with empty watermark text")
    void testEmbedWithEmptyWatermark() throws Exception {
        // Given
        MockMultipartFile imageFile = TestDataFactory.createDefaultTestImageFile();
        
        // When/Then
        mockMvc.perform(multipart("/api/v1/watermark/embed")
                        .file(imageFile)
                        .param("watermarkText", "")
                        .param("algorithm", "DCT")
                        .param("strength", "50"))
                .andExpect(status().is4xxClientError());
    }
    
    @Test
    @DisplayName("POST /embed should fail with too long watermark text")
    void testEmbedWithTooLongWatermark() throws Exception {
        // Given
        MockMultipartFile imageFile = TestDataFactory.createDefaultTestImageFile();
        String tooLongText = TestDataFactory.createTooLongWatermarkText(); // 65 chars
        
        // When/Then
        mockMvc.perform(multipart("/api/v1/watermark/embed")
                        .file(imageFile)
                        .param("watermarkText", tooLongText)
                        .param("algorithm", "DCT")
                        .param("strength", "50"))
                .andExpect(status().is4xxClientError());
    }
    
    @Test
    @DisplayName("POST /extract should extract watermark from embedded image")
    void testExtractWatermark() throws Exception {
        // Given: First embed a watermark
        MockMultipartFile originalImage = TestDataFactory.createDefaultTestImageFile();
        String expectedWatermark = "TEST_EXTRACT";
        
        byte[] watermarkedImageBytes = mockMvc.perform(multipart("/api/v1/watermark/embed")
                        .file(originalImage)
                        .param("watermarkText", expectedWatermark)
                        .param("algorithm", "DCT")
                        .param("strength", "50"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        
        MockMultipartFile watermarkedImage = new MockMultipartFile(
                "image", "watermarked.png", "image/png", watermarkedImageBytes
        );
        
        // When: Extract watermark
        mockMvc.perform(multipart("/api/v1/watermark/extract")
                        .file(watermarkedImage)
                        .param("algorithm", "DCT")
                        .param("strength", "50"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.extractedWatermark").value(expectedWatermark))
                .andExpect(jsonPath("$.data.algorithm").value("DCT"))
                .andExpect(jsonPath("$.data.confidence").isNumber())
                .andExpect(jsonPath("$.data.ber").isNumber())
                .andExpect(jsonPath("$.data.status").exists());
    }
    
    @Test
    @DisplayName("POST /extract should fail with wrong strength parameter")
    void testExtractWithWrongStrength() throws Exception {
        // Given: Embed with strength 50
        MockMultipartFile originalImage = TestDataFactory.createDefaultTestImageFile();
        
        byte[] watermarkedImageBytes = mockMvc.perform(multipart("/api/v1/watermark/embed")
                        .file(originalImage)
                        .param("watermarkText", "TEST")
                        .param("algorithm", "DCT")
                        .param("strength", "50"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        
        MockMultipartFile watermarkedImage = new MockMultipartFile(
                "image", "watermarked.png", "image/png", watermarkedImageBytes
        );
        
        // When: Extract with different strength (70)
        mockMvc.perform(multipart("/api/v1/watermark/extract")
                        .file(watermarkedImage)
                        .param("algorithm", "DCT")
                        .param("strength", "70"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.extractedWatermark").value(not("TEST"))); // Wrong extraction
    }
    
    @Test
    @DisplayName("POST /evaluate should return quality metrics")
    void testEvaluateQuality() throws Exception {
        // Given
        MockMultipartFile originalImage = TestDataFactory.createDefaultTestImageFile();
        
        byte[] watermarkedImageBytes = mockMvc.perform(multipart("/api/v1/watermark/embed")
                        .file(originalImage)
                        .param("watermarkText", "TEST")
                        .param("algorithm", "DCT")
                        .param("strength", "50"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        
        MockMultipartFile watermarkedImage = new MockMultipartFile(
                "watermarkedImage", "watermarked.png", "image/png", watermarkedImageBytes
        );
        
        // When: Evaluate quality
        mockMvc.perform(multipart("/api/v1/watermark/evaluate")
                        .file("originalImage", originalImage.getBytes())
                        .file(watermarkedImage))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.psnr").isNumber())
                .andExpect(jsonPath("$.data.ssim").isNumber())
                .andExpect(jsonPath("$.data.psnr").value(greaterThan(30.0))) // Reasonable PSNR
                .andExpect(jsonPath("$.data.ssim").value(greaterThan(0.9))); // High SSIM
    }
    
    @Test
    @DisplayName("POST /robustness-test should test watermark against attacks")
    void testRobustnessTest() throws Exception {
        // Given
        MockMultipartFile originalImage = TestDataFactory.createDefaultTestImageFile();
        
        byte[] watermarkedImageBytes = mockMvc.perform(multipart("/api/v1/watermark/embed")
                        .file(originalImage)
                        .param("watermarkText", "ROBUST_TEST")
                        .param("algorithm", "DCT")
                        .param("strength", "60"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        
        MockMultipartFile watermarkedImage = new MockMultipartFile(
                "watermarkedImage", "watermarked.png", "image/png", watermarkedImageBytes
        );
        
        // When: Test robustness with multiple attacks
        mockMvc.perform(multipart("/api/v1/watermark/robustness-test")
                        .file("originalImage", originalImage.getBytes())
                        .file(watermarkedImage)
                        .param("attacks", "JPEG_70,RESIZE_75,GAUSSIAN_NOISE")
                        .param("algorithm", "DCT")
                        .param("strength", "60"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalAttacks").value(3))
                .andExpect(jsonPath("$.data.passedAttacks").isNumber())
                .andExpect(jsonPath("$.data.failedAttacks").isNumber())
                .andExpect(jsonPath("$.data.successRate").isNumber())
                .andExpect(jsonPath("$.data.attackResults").isArray())
                .andExpect(jsonPath("$.data.attackResults", hasSize(3)))
                .andExpect(jsonPath("$.data.attackResults[0].attackType").exists())
                .andExpect(jsonPath("$.data.attackResults[0].passed").isBoolean());
    }
    
    @Test
    @DisplayName("Should handle empty image file")
    void testEmbedWithEmptyImage() throws Exception {
        // Given
        MockMultipartFile emptyFile = TestDataFactory.createEmptyMultipartFile();
        
        // When/Then
        mockMvc.perform(multipart("/api/v1/watermark/embed")
                        .file(emptyFile)
                        .param("watermarkText", "TEST")
                        .param("algorithm", "DCT")
                        .param("strength", "50"))
                .andExpect(status().is4xxClientError());
    }
    
    @Test
    @DisplayName("Should handle invalid image format")
    void testEmbedWithInvalidImage() throws Exception {
        // Given
        MockMultipartFile invalidFile = TestDataFactory.createInvalidMultipartFile();
        
        // When/Then
        mockMvc.perform(multipart("/api/v1/watermark/embed")
                        .file(invalidFile)
                        .param("watermarkText", "TEST")
                        .param("algorithm", "DCT")
                        .param("strength", "50"))
                .andExpect(status().is4xxClientError());
    }
    
    @Test
    @DisplayName("Should use default parameters when not specified")
    void testEmbedWithDefaultParameters() throws Exception {
        // Given
        MockMultipartFile imageFile = TestDataFactory.createDefaultTestImageFile();
        
        // When: No algorithm and strength specified
        mockMvc.perform(multipart("/api/v1/watermark/embed")
                        .file(imageFile)
                        .param("watermarkText", "TEST"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Algorithm", "DCT")) // Default algorithm
                .andExpect(header().string("X-Strength", "50")); // Default strength
    }
}
