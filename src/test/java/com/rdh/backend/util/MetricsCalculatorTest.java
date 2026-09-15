package com.rdh.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests cho MetricsCalculator
 * Test PSNR, SSIM, và Capacity calculations
 */
@DisplayName("MetricsCalculator Unit Tests")
class MetricsCalculatorTest {
    
    @Nested
    @DisplayName("PSNR Calculation")
    class PsnrTests {
        
        @Test
        @DisplayName("PSNR of identical images should be infinity")
        void testPsnrIdenticalImages() {
            // Given: Two identical images
            BufferedImage img1 = TestImageGenerator.createGradientImage(128, 128);
            BufferedImage img2 = TestImageGenerator.cloneImage(img1);
            
            // When: Calculate PSNR
            double psnr = MetricsCalculator.calculatePSNR(img1, img2);
            
            // Then: PSNR should be very high (approaching infinity)
            assertThat(psnr).isGreaterThan(100.0); // In practice, returns a large number
        }
        
        @Test
        @DisplayName("PSNR should be high for very similar images")
        void testPsnrSimilarImages() {
            // Given: Two very similar images (1 pixel difference)
            BufferedImage img1 = TestImageGenerator.createSolidColorImage(64, 64, java.awt.Color.BLACK);
            BufferedImage img2 = TestImageGenerator.cloneImage(img1);
            img2.setRGB(0, 0, 0x010101); // Change 1 pixel slightly
            
            // When: Calculate PSNR
            double psnr = MetricsCalculator.calculatePSNR(img1, img2);
            
            // Then: PSNR should be very high (> 50 dB)
            assertThat(psnr).isGreaterThan(50.0);
        }
        
        @Test
        @DisplayName("PSNR should be low for very different images")
        void testPsnrDifferentImages() {
            // Given: Two completely different images
            BufferedImage img1 = TestImageGenerator.createSolidColorImage(64, 64, java.awt.Color.BLACK);
            BufferedImage img2 = TestImageGenerator.createSolidColorImage(64, 64, java.awt.Color.WHITE);
            
            // When: Calculate PSNR
            double psnr = MetricsCalculator.calculatePSNR(img1, img2);
            
            // Then: PSNR should be very low
            assertThat(psnr).isLessThan(10.0);
        }
        
        @Test
        @DisplayName("PSNR should handle grayscale images")
        void testPsnrGrayscaleImages() {
            // Given: Grayscale images
            BufferedImage img1 = TestImageGenerator.createGradientImage(100, 100);
            BufferedImage img2 = TestImageGenerator.cloneImage(img1);
            
            // Modify slightly
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < 10; x++) {
                    int rgb = img2.getRGB(x, y);
                    int gray = (rgb >> 16) & 0xFF;
                    int newGray = Math.min(255, gray + 5);
                    int newRgb = (newGray << 16) | (newGray << 8) | newGray;
                    img2.setRGB(x, y, newRgb);
                }
            }
            
            // When: Calculate PSNR
            double psnr = MetricsCalculator.calculatePSNR(img1, img2);
            
            // Then: PSNR should be in reasonable range
            assertThat(psnr).isBetween(30.0, 60.0);
        }
    }
    
    @Nested
    @DisplayName("SSIM Calculation")
    class SsimTests {
        
        @Test
        @DisplayName("SSIM of identical images should be 1.0")
        void testSsimIdenticalImages() {
            // Given: Two identical images
            BufferedImage img1 = TestImageGenerator.createGradientImage(128, 128);
            BufferedImage img2 = TestImageGenerator.cloneImage(img1);
            
            // When: Calculate SSIM
            double ssim = MetricsCalculator.calculateSSIM(img1, img2);
            
            // Then: SSIM should be 1.0 (or very close)
            assertThat(ssim).isCloseTo(1.0, within(0.01));
        }
        
        @Test
        @DisplayName("SSIM should be high for similar images")
        void testSsimSimilarImages() {
            // Given: Similar images
            BufferedImage img1 = TestImageGenerator.createGradientImage(100, 100);
            BufferedImage img2 = TestImageGenerator.cloneImage(img1);
            
            // Add small noise
            for (int y = 0; y < img2.getHeight(); y++) {
                for (int x = 0; x < img2.getWidth(); x++) {
                    if (Math.random() < 0.05) { // 5% noise
                        int rgb = img2.getRGB(x, y);
                        int r = Math.min(255, ((rgb >> 16) & 0xFF) + 5);
                        int newRgb = (r << 16) | ((rgb >> 8) & 0xFF) << 8 | (rgb & 0xFF);
                        img2.setRGB(x, y, newRgb);
                    }
                }
            }
            
            // When: Calculate SSIM
            double ssim = MetricsCalculator.calculateSSIM(img1, img2);
            
            // Then: SSIM should be > 0.95
            assertThat(ssim).isGreaterThan(0.95);
        }
        
        @Test
        @DisplayName("SSIM should be low for different images")
        void testSsimDifferentImages() {
            // Given: Two different images
            BufferedImage img1 = TestImageGenerator.createCheckerboardImage(64, 64, 8);
            BufferedImage img2 = TestImageGenerator.createGradientImage(64, 64);
            
            // When: Calculate SSIM
            double ssim = MetricsCalculator.calculateSSIM(img1, img2);
            
            // Then: SSIM should be < 0.8
            assertThat(ssim).isLessThan(0.8);
        }
        
        @Test
        @DisplayName("SSIM should be between 0 and 1")
        void testSsimRange() {
            // Given: Random images
            BufferedImage img1 = TestImageGenerator.createRandomNoiseImage(100, 100);
            BufferedImage img2 = TestImageGenerator.createRandomNoiseImage(100, 100);
            
            // When: Calculate SSIM
            double ssim = MetricsCalculator.calculateSSIM(img1, img2);
            
            // Then: SSIM should be in valid range
            assertThat(ssim).isBetween(0.0, 1.0);
        }
    }
    
    @Nested
    @DisplayName("Capacity Calculation")
    class CapacityTests {
        
        @Test
        @DisplayName("Capacity should be positive for valid image")
        void testCapacityPositive() {
            // Given: Standard test image
            BufferedImage image = TestImageGenerator.createHistogramShiftingTestImage(128, 128);
            
            // When: Calculate capacity
            long capacity = MetricsCalculator.calculateCapacity(image);
            
            // Then: Capacity should be > 0
            assertThat(capacity).isGreaterThan(0L);
        }
        
        @Test
        @DisplayName("Capacity should increase with image size")
        void testCapacityScalesWithSize() {
            // Given: Images of different sizes
            BufferedImage small = TestImageGenerator.createHistogramShiftingTestImage(64, 64);
            BufferedImage large = TestImageGenerator.createHistogramShiftingTestImage(128, 128);
            
            // When: Calculate capacities
            long capacitySmall = MetricsCalculator.calculateCapacity(small);
            long capacityLarge = MetricsCalculator.calculateCapacity(large);
            
            // Then: Larger image should have higher capacity
            assertThat(capacityLarge).isGreaterThan(capacitySmall);
        }
        
        @Test
        @DisplayName("Capacity should depend on histogram characteristics")
        void testCapacityDependsOnHistogram() {
            // Given: Image with high peak vs flat histogram
            BufferedImage highPeak = TestImageGenerator.createSolidColorImage(100, 100, java.awt.Color.GRAY);
            BufferedImage flatHist = TestImageGenerator.createRandomNoiseImage(100, 100);
            
            // When: Calculate capacities
            long capacityHighPeak = MetricsCalculator.calculateCapacity(highPeak);
            long capacityFlat = MetricsCalculator.calculateCapacity(flatHist);
            
            // Then: High peak image should have higher capacity
            assertThat(capacityHighPeak).isGreaterThan(capacityFlat);
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle small images")
        void testSmallImages() {
            // Given: Very small image
            BufferedImage img1 = TestImageGenerator.createSolidColorImage(16, 16, java.awt.Color.RED);
            BufferedImage img2 = TestImageGenerator.cloneImage(img1);
            
            // When: Calculate metrics
            double psnr = MetricsCalculator.calculatePSNR(img1, img2);
            double ssim = MetricsCalculator.calculateSSIM(img1, img2);
            
            // Then: Should not throw exceptions
            assertThat(psnr).isGreaterThan(50.0);
            assertThat(ssim).isCloseTo(1.0, within(0.01));
        }
        
        @Test
        @DisplayName("Should handle non-square images")
        void testNonSquareImages() {
            // Given: Non-square images
            BufferedImage img1 = TestImageGenerator.createGradientImage(100, 50);
            BufferedImage img2 = TestImageGenerator.cloneImage(img1);
            
            // When: Calculate metrics
            double psnr = MetricsCalculator.calculatePSNR(img1, img2);
            double ssim = MetricsCalculator.calculateSSIM(img1, img2);
            long capacity = MetricsCalculator.calculateCapacity(img1);
            
            // Then: Should work correctly
            assertThat(psnr).isGreaterThan(50.0);
            assertThat(ssim).isCloseTo(1.0, within(0.01));
            assertThat(capacity).isGreaterThan(0L);
        }
    }
}
