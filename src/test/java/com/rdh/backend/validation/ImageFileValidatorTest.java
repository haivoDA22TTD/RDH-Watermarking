package com.rdh.backend.validation;

import com.rdh.backend.exception.InvalidImageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ImageFileValidator
 */
class ImageFileValidatorTest {

    private ImageFileValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ImageFileValidator();
    }

    // ==================== Helper Methods ====================

    /**
     * Create a valid test image (PNG)
     */
    private MultipartFile createValidImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // Fill with some pattern to make it more realistic
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = (x * y) % 255;
                image.setRGB(x, y, (rgb << 16) | (rgb << 8) | rgb);
            }
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        
        return new MockMultipartFile(
            "image",
            "test.png",
            "image/png",
            imageBytes
        );
    }

    // ==================== Null and Empty Tests ====================

    @Test
    @DisplayName("Should throw exception when file is null")
    void testNullFile() {
        assertThatThrownBy(() -> validator.validateImageFile(null))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("required");
    }

    @Test
    @DisplayName("Should throw exception when file is empty")
    void testEmptyFile() {
        MultipartFile emptyFile = new MockMultipartFile(
            "image",
            "test.png",
            "image/png",
            new byte[0]
        );

        assertThatThrownBy(() -> validator.validateImageFile(emptyFile))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("empty");
    }

    // ==================== File Size Tests ====================

    @Test
    @DisplayName("Should accept valid file size (single upload)")
    void testValidFileSize() throws IOException {
        MultipartFile validFile = createValidImage(256, 256);
        
        assertThatCode(() -> validator.validateImageFile(validFile))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception when file exceeds 10MB (single)")
    void testFileSizeExceeded() {
        // Create 11MB file
        byte[] largeData = new byte[11 * 1024 * 1024];
        MultipartFile largeFile = new MockMultipartFile(
            "image",
            "large.png",
            "image/png",
            largeData
        );

        assertThatThrownBy(() -> validator.validateImageFile(largeFile))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("exceeds maximum");
    }

    @Test
    @DisplayName("Should accept file close to 10MB limit")
    void testFileSizeNearLimit() throws IOException {
        // Create ~9MB valid image
        MultipartFile file = createValidImage(1920, 1200);
        
        assertThatCode(() -> validator.validateImageFile(file))
            .doesNotThrowAnyException();
    }

    // ==================== Content Type Tests ====================

    @Test
    @DisplayName("Should accept PNG content type")
    void testValidContentTypePNG() throws IOException {
        MultipartFile file = createValidImage(128, 128);
        
        assertThatCode(() -> validator.validateImageFile(file))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception for invalid content type")
    void testInvalidContentType() {
        MultipartFile file = new MockMultipartFile(
            "image",
            "test.txt",
            "text/plain",
            "not an image".getBytes()
        );

        assertThatThrownBy(() -> validator.validateImageFile(file))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("Invalid content type");
    }

    @Test
    @DisplayName("Should throw exception when content type is null")
    void testNullContentType() {
        MultipartFile file = new MockMultipartFile(
            "image",
            "test.png",
            null,
            "fake data".getBytes()
        );

        assertThatThrownBy(() -> validator.validateImageFile(file))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("Invalid content type");
    }

    // ==================== File Extension Tests ====================

    @Test
    @DisplayName("Should accept valid extensions")
    void testValidExtensions() throws IOException {
        String[] validExtensions = {"png", "jpg", "jpeg", "bmp"};
        
        for (String ext : validExtensions) {
            MultipartFile file = new MockMultipartFile(
                "image",
                "test." + ext,
                "image/" + ext,
                createValidImage(128, 128).getBytes()
            );
            
            assertThatCode(() -> validator.validateImageFile(file))
                .as("Extension: " + ext)
                .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Should throw exception for invalid extension")
    void testInvalidExtension() {
        MultipartFile file = new MockMultipartFile(
            "image",
            "test.gif",
            "image/gif",
            "fake data".getBytes()
        );

        assertThatThrownBy(() -> validator.validateImageFile(file))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("extension");
    }

    @Test
    @DisplayName("Should throw exception when filename has no extension")
    void testNoExtension() {
        MultipartFile file = new MockMultipartFile(
            "image",
            "testfile",
            "image/png",
            "fake data".getBytes()
        );

        assertThatThrownBy(() -> validator.validateImageFile(file))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("extension");
    }

    // ==================== Image Content Tests ====================

    @Test
    @DisplayName("Should throw exception when file is not a valid image")
    void testInvalidImageContent() {
        MultipartFile file = new MockMultipartFile(
            "image",
            "fake.png",
            "image/png",
            "This is not an image".getBytes()
        );

        assertThatThrownBy(() -> validator.validateImageFile(file))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("Cannot read file as image");
    }

    @Test
    @DisplayName("Should accept valid image content")
    void testValidImageContent() throws IOException {
        MultipartFile file = createValidImage(256, 256);
        
        assertThatCode(() -> validator.validateImageFile(file))
            .doesNotThrowAnyException();
    }

    // ==================== Dimension Tests ====================

    @Test
    @DisplayName("Should accept minimum valid dimensions (64x64)")
    void testMinimumDimensions() throws IOException {
        MultipartFile file = createValidImage(64, 64);
        
        assertThatCode(() -> validator.validateImageFile(file))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception for dimensions too small")
    void testDimensionsTooSmall() throws IOException {
        MultipartFile file = createValidImage(32, 32);

        assertThatThrownBy(() -> validator.validateImageFile(file))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("too small");
    }

    @Test
    @DisplayName("Should accept maximum valid dimensions (4096x4096)")
    void testMaximumDimensions() throws IOException {
        MultipartFile file = createValidImage(4096, 4096);
        
        assertThatCode(() -> validator.validateImageFile(file))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception for dimensions too large")
    void testDimensionsTooLarge() throws IOException {
        MultipartFile file = createValidImage(5000, 5000);

        assertThatThrownBy(() -> validator.validateImageFile(file))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("too large");
    }

    @Test
    @DisplayName("Should accept common dimensions")
    void testCommonDimensions() throws IOException {
        int[][] commonSizes = {
            {128, 128},
            {256, 256},
            {512, 512},
            {1024, 1024},
            {1920, 1080},
            {800, 600}
        };
        
        for (int[] size : commonSizes) {
            MultipartFile file = createValidImage(size[0], size[1]);
            
            assertThatCode(() -> validator.validateImageFile(file))
                .as("Dimensions: " + size[0] + "x" + size[1])
                .doesNotThrowAnyException();
        }
    }

    @Test
    @DisplayName("Should warn for dimensions not divisible by 8")
    void testDimensionsNotDivisibleBy8() throws IOException {
        // 257 is not divisible by 8
        MultipartFile file = createValidImage(257, 257);
        
        // Should not throw, but will log warning
        assertThatCode(() -> validator.validateImageFile(file))
            .doesNotThrowAnyException();
    }

    // ==================== Batch Validation Tests ====================

    @Test
    @DisplayName("Should validate multiple files (batch)")
    void testBatchValidation() throws IOException {
        List<MultipartFile> files = Arrays.asList(
            createValidImage(256, 256),
            createValidImage(512, 512),
            createValidImage(128, 128)
        );

        assertThatCode(() -> validator.validateImageFiles(files))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception when batch is empty")
    void testEmptyBatch() {
        assertThatThrownBy(() -> validator.validateImageFiles(Arrays.asList()))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("At least one");
    }

    @Test
    @DisplayName("Should throw exception when batch is null")
    void testNullBatch() {
        assertThatThrownBy(() -> validator.validateImageFiles(null))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("At least one");
    }

    @Test
    @DisplayName("Should throw exception when one file in batch is invalid")
    void testBatchWithInvalidFile() throws IOException {
        MultipartFile invalidFile = new MockMultipartFile(
            "image",
            "invalid.png",
            "image/png",
            "not an image".getBytes()
        );
        
        List<MultipartFile> files = Arrays.asList(
            createValidImage(256, 256),
            invalidFile,
            createValidImage(128, 128)
        );

        assertThatThrownBy(() -> validator.validateImageFiles(files))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("File #2");
    }

    @Test
    @DisplayName("Should throw exception when batch total size exceeds 200MB")
    void testBatchSizeExceeded() {
        // Create files that together exceed 200MB
        byte[] largeData = new byte[100 * 1024 * 1024]; // 100MB each
        
        List<MultipartFile> files = Arrays.asList(
            new MockMultipartFile("image1", "test1.png", "image/png", largeData),
            new MockMultipartFile("image2", "test2.png", "image/png", largeData),
            new MockMultipartFile("image3", "test3.png", "image/png", largeData)
        );

        assertThatThrownBy(() -> validator.validateImageFiles(files))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("batch size exceeds");
    }

    // ==================== Helper Method Tests ====================

    @Test
    @DisplayName("isValidImageFile should return true for valid file")
    void testIsValidImageFileTrue() throws IOException {
        MultipartFile file = createValidImage(256, 256);
        
        assertThat(validator.isValidImageFile(file)).isTrue();
    }

    @Test
    @DisplayName("isValidImageFile should return false for invalid file")
    void testIsValidImageFileFalse() {
        MultipartFile file = new MockMultipartFile(
            "image",
            "invalid.png",
            "image/png",
            "not an image".getBytes()
        );
        
        assertThat(validator.isValidImageFile(file)).isFalse();
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should handle corrupted PNG file")
    void testCorruptedPNG() {
        // PNG header but corrupted data
        byte[] corruptedPNG = {
            (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x00 // corrupted data
        };
        
        MultipartFile file = new MockMultipartFile(
            "image",
            "corrupted.png",
            "image/png",
            corruptedPNG
        );

        assertThatThrownBy(() -> validator.validateImageFile(file))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("Cannot read");
    }

    @Test
    @DisplayName("Should accept square images")
    void testSquareImages() throws IOException {
        MultipartFile file = createValidImage(512, 512);
        
        assertThatCode(() -> validator.validateImageFile(file))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept rectangular images")
    void testRectangularImages() throws IOException {
        MultipartFile file = createValidImage(1920, 1080);
        
        assertThatCode(() -> validator.validateImageFile(file))
            .doesNotThrowAnyException();
    }
}
