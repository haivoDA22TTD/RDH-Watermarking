package com.rdh.backend.strategy.watermark;

import com.rdh.backend.util.TestImageGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests cho DctWatermarkStrategy
 * Test embed and extract operations
 */
@DisplayName("DctWatermarkStrategy Unit Tests")
class DctWatermarkStrategyTest {
    
    private DctWatermarkStrategy strategy;
    
    @BeforeEach
    void setUp() {
        strategy = new DctWatermarkStrategy();
    }
    
    @Nested
    @DisplayName("Basic Embed/Extract")
    class BasicEmbedExtractTests {
        
        @Test
        @DisplayName("Should embed and extract watermark correctly")
        void testBasicEmbedExtract() {
            // Given
            BufferedImage originalImage = TestImageGenerator.createStandardTestImage();
            String watermarkText = "TEST_WATERMARK";
            int strength = 50;
            
            // When: Embed
            BufferedImage watermarkedImage = strategy.embed(originalImage, watermarkText, strength);
            
            // Then: Watermarked image should be created
            assertThat(watermarkedImage).isNotNull();
            assertThat(watermarkedImage.getWidth()).isEqualTo(originalImage.getWidth());
            assertThat(watermarkedImage.getHeight()).isEqualTo(originalImage.getHeight());
            
            // When: Extract
            String extractedWatermark = strategy.extract(watermarkedImage, strength);
            
            // Then: Should recover original watermark
            assertThat(extractedWatermark).isEqualTo(watermarkText);
        }
        
        @Test
        @DisplayName("Should handle short watermark text")
        void testShortWatermark() {
            // Given
            BufferedImage image = TestImageGenerator.createSmallTestImage();
            String watermarkText = "ABC";
            
            // When: Embed and extract
            BufferedImage watermarked = strategy.embed(image, watermarkText, 50);
            String extracted = strategy.extract(watermarked, 50);
            
            // Then
            assertThat(extracted).isEqualTo(watermarkText);
        }
        
        @Test
        @DisplayName("Should handle long watermark text")
        void testLongWatermark() {
            // Given
            BufferedImage image = TestImageGenerator.createLargeTestImage();
            String watermarkText = "A".repeat(50); // 50 characters
            
            // When: Embed and extract
            BufferedImage watermarked = strategy.embed(image, watermarkText, 50);
            String extracted = strategy.extract(watermarked, 50);
            
            // Then
            assertThat(extracted).isEqualTo(watermarkText);
        }
        
        @Test
        @DisplayName("Should handle watermark with special characters")
        void testSpecialCharacters() {
            // Given
            BufferedImage image = TestImageGenerator.createStandardTestImage();
            String watermarkText = "COPYRIGHT©2024";
            
            // When: Embed and extract
            BufferedImage watermarked = strategy.embed(image, watermarkText, 50);
            String extracted = strategy.extract(watermarked, 50);
            
            // Then
            assertThat(extracted).isEqualTo(watermarkText);
        }
    }
    
    @Nested
    @DisplayName("Strength Parameter")
    class StrengthTests {
        
        @Test
        @DisplayName("Low strength should produce high PSNR")
        void testLowStrength() {
            // Given
            BufferedImage original = TestImageGenerator.createStandardTestImage();
            String watermark = "TEST";
            
            // When: Embed with low strength
            BufferedImage watermarked = strategy.embed(original, watermark, 20);
            
            // Then: Calculate PSNR (should be high)
            double mse = TestImageGenerator.calculateMSE(original, watermarked);
            double psnr = 10 * Math.log10((255 * 255) / mse);
            
            assertThat(psnr).isGreaterThan(45.0); // High PSNR
        }
        
        @Test
        @DisplayName("High strength should produce lower PSNR but more robust")
        void testHighStrength() {
            // Given
            BufferedImage original = TestImageGenerator.createStandardTestImage();
            String watermark = "TEST";
            
            // When: Embed with high strength
            BufferedImage watermarked = strategy.embed(original, watermark, 80);
            
            // Then: Calculate PSNR (should be lower)
            double mse = TestImageGenerator.calculateMSE(original, watermarked);
            double psnr = 10 * Math.log10((255 * 255) / mse);
            
            assertThat(psnr).isBetween(30.0, 50.0); // Lower PSNR but still acceptable
        }
        
        @Test
        @DisplayName("Extract should fail if strength mismatch")
        void testStrengthMismatch() {
            // Given
            BufferedImage image = TestImageGenerator.createStandardTestImage();
            String watermark = "TEST_WATERMARK";
            
            // When: Embed with strength 50
            BufferedImage watermarked = strategy.embed(image, watermark, 50);
            
            // Extract with different strength
            String extracted = strategy.extract(watermarked, 70);
            
            // Then: Extracted watermark should be different (corrupted)
            assertThat(extracted).isNotEqualTo(watermark);
        }
    }
    
    @Nested
    @DisplayName("Image Properties")
    class ImagePropertiesTests {
        
        @Test
        @DisplayName("Should preserve image dimensions")
        void testPreserveDimensions() {
            // Given
            BufferedImage original = TestImageGenerator.createStandardTestImage();
            
            // When: Embed watermark
            BufferedImage watermarked = strategy.embed(original, "TEST", 50);
            
            // Then: Dimensions should match
            assertThat(watermarked.getWidth()).isEqualTo(original.getWidth());
            assertThat(watermarked.getHeight()).isEqualTo(original.getHeight());
            assertThat(watermarked.getType()).isEqualTo(original.getType());
        }
        
        @Test
        @DisplayName("Should work with different image types")
        void testDifferentImageTypes() {
            // Given: Images with different patterns
            BufferedImage gradient = TestImageGenerator.createGradientImage(128, 128);
            BufferedImage checkerboard = TestImageGenerator.createCheckerboardImage(128, 128, 8);
            BufferedImage noise = TestImageGenerator.createRandomNoiseImage(128, 128);
            
            String watermark = "TEST";
            
            // When: Embed in each
            BufferedImage w1 = strategy.embed(gradient, watermark, 50);
            BufferedImage w2 = strategy.embed(checkerboard, watermark, 50);
            BufferedImage w3 = strategy.embed(noise, watermark, 50);
            
            // Then: Should extract from all
            assertThat(strategy.extract(w1, 50)).isEqualTo(watermark);
            assertThat(strategy.extract(w2, 50)).isEqualTo(watermark);
            assertThat(strategy.extract(w3, 50)).isEqualTo(watermark);
        }
        
        @Test
        @DisplayName("Should handle non-square images")
        void testNonSquareImages() {
            // Given
            BufferedImage image = TestImageGenerator.createGradientImage(256, 128); // 2:1 ratio
            String watermark = "TEST_WATERMARK";
            
            // When: Embed and extract
            BufferedImage watermarked = strategy.embed(image, watermark, 50);
            String extracted = strategy.extract(watermarked, 50);
            
            // Then
            assertThat(extracted).isEqualTo(watermark);
        }
    }
    
    @Nested
    @DisplayName("Delimiter Detection")
    class DelimiterTests {
        
        @Test
        @DisplayName("Should detect delimiter and stop extraction")
        void testDelimiterDetection() {
            // Given
            BufferedImage image = TestImageGenerator.createStandardTestImage();
            String watermark = "SHORT";
            
            // When: Embed and extract
            BufferedImage watermarked = strategy.embed(image, watermark, 50);
            String extracted = strategy.extract(watermarked, 50);
            
            // Then: Should only return watermark before delimiter
            assertThat(extracted).isEqualTo(watermark);
            assertThat(extracted).doesNotContain("###END###");
        }
    }
    
    @Nested
    @DisplayName("Algorithm Name")
    class AlgorithmNameTest {
        
        @Test
        @DisplayName("Algorithm name should be DCT")
        void testAlgorithmName() {
            // When
            String name = strategy.getAlgorithmName();
            
            // Then
            assertThat(name).isEqualTo("DCT");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle minimum size image")
        void testMinimumSizeImage() {
            // Given: 64x64 image (8 blocks x 8 blocks)
            BufferedImage image = TestImageGenerator.createSolidColorImage(64, 64, java.awt.Color.GRAY);
            String watermark = "TEST";
            
            // When: Embed and extract
            BufferedImage watermarked = strategy.embed(image, watermark, 50);
            String extracted = strategy.extract(watermarked, 50);
            
            // Then
            assertThat(extracted).isEqualTo(watermark);
        }
        
        @Test
        @DisplayName("Should not crash on empty watermark")
        void testEmptyWatermark() {
            // Given
            BufferedImage image = TestImageGenerator.createStandardTestImage();
            String watermark = "";
            
            // When/Then: Should not throw exception
            assertThatCode(() -> {
                BufferedImage watermarked = strategy.embed(image, watermark, 50);
                strategy.extract(watermarked, 50);
            }).doesNotThrowAnyException();
        }
    }
}
