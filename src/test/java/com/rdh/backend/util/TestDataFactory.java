package com.rdh.backend.util;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Factory class để tạo test data (MultipartFile, test strings, etc.)
 */
public class TestDataFactory {
    
    /**
     * Convert BufferedImage sang MockMultipartFile
     */
    public static MockMultipartFile createMultipartFile(
            BufferedImage image, 
            String filename, 
            String format
    ) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        byte[] bytes = baos.toByteArray();
        
        return new MockMultipartFile(
            "image",
            filename,
            "image/" + format.toLowerCase(),
            bytes
        );
    }
    
    /**
     * Tạo MockMultipartFile PNG
     */
    public static MockMultipartFile createPngMultipartFile(BufferedImage image, String filename) throws IOException {
        return createMultipartFile(image, filename, "png");
    }
    
    /**
     * Tạo MockMultipartFile JPG
     */
    public static MockMultipartFile createJpgMultipartFile(BufferedImage image, String filename) throws IOException {
        return createMultipartFile(image, filename, "jpg");
    }
    
    /**
     * Tạo test image file mặc định
     */
    public static MockMultipartFile createDefaultTestImageFile() throws IOException {
        BufferedImage image = TestImageGenerator.createStandardTestImage();
        return createPngMultipartFile(image, "test-image.png");
    }
    
    /**
     * Tạo small test image file
     */
    public static MockMultipartFile createSmallTestImageFile() throws IOException {
        BufferedImage image = TestImageGenerator.createSmallTestImage();
        return createPngMultipartFile(image, "small-test.png");
    }
    
    /**
     * Tạo large test image file
     */
    public static MockMultipartFile createLargeTestImageFile() throws IOException {
        BufferedImage image = TestImageGenerator.createLargeTestImage();
        return createPngMultipartFile(image, "large-test.png");
    }
    
    /**
     * Tạo empty MultipartFile (để test validation)
     */
    public static MockMultipartFile createEmptyMultipartFile() {
        return new MockMultipartFile(
            "image",
            "empty.png",
            "image/png",
            new byte[0]
        );
    }
    
    /**
     * Tạo invalid MultipartFile (không phải ảnh)
     */
    public static MockMultipartFile createInvalidMultipartFile() {
        return new MockMultipartFile(
            "image",
            "invalid.txt",
            "text/plain",
            "This is not an image".getBytes()
        );
    }
    
    /**
     * Tạo test watermark text
     */
    public static String createTestWatermarkText(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((char) ('A' + (i % 26)));
        }
        return sb.toString();
    }
    
    /**
     * Tạo watermark text mặc định
     */
    public static String createDefaultWatermarkText() {
        return "TEST_WATERMARK_2024";
    }
    
    /**
     * Tạo short watermark text
     */
    public static String createShortWatermarkText() {
        return "TEST";
    }
    
    /**
     * Tạo long watermark text (64 chars - max allowed)
     */
    public static String createLongWatermarkText() {
        return "A".repeat(64);
    }
    
    /**
     * Tạo too long watermark text (65 chars - should fail validation)
     */
    public static String createTooLongWatermarkText() {
        return "A".repeat(65);
    }
    
    /**
     * Tạo watermark text với special characters
     */
    public static String createSpecialCharWatermarkText() {
        return "COPYRIGHT©2024®™";
    }
}
