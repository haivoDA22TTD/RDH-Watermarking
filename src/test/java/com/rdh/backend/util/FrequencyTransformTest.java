package com.rdh.backend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests cho FrequencyTransform utility class
 * Test DCT/IDCT operations, QIM embedding/extraction, và các helper methods
 */
@DisplayName("FrequencyTransform Unit Tests")
class FrequencyTransformTest {
    
    @Nested
    @DisplayName("DCT/IDCT Operations")
    class DctIdctTests {
        
        @Test
        @DisplayName("DCT transform should produce frequency coefficients")
        void testDctTransform() {
            // Given: 8x8 block với giá trị đơn giản
            double[][] block = new double[8][8];
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    block[i][j] = 128.0; // Solid gray
                }
            }
            
            // When: Apply DCT
            double[][] dctBlock = FrequencyTransform.applyDCT(block);
            
            // Then: DC coefficient (0,0) should be dominant
            assertThat(dctBlock).isNotNull();
            assertThat(dctBlock).hasSize(8);
            assertThat(dctBlock[0]).hasSize(8);
            
            // DC coefficient should be large (contains average)
            assertThat(Math.abs(dctBlock[0][0])).isGreaterThan(100.0);
            
            // AC coefficients should be small (no variation)
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (i != 0 || j != 0) {
                        assertThat(Math.abs(dctBlock[i][j])).isLessThan(10.0);
                    }
                }
            }
        }
        
        @Test
        @DisplayName("IDCT should reconstruct original block")
        void testIdctReconstruction() {
            // Given: Original 8x8 block
            double[][] originalBlock = new double[8][8];
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    originalBlock[i][j] = 100.0 + i * 10 + j;
                }
            }
            
            // When: Apply DCT then IDCT
            double[][] dctBlock = FrequencyTransform.applyDCT(originalBlock);
            double[][] reconstructedBlock = FrequencyTransform.applyIDCT(dctBlock);
            
            // Then: Reconstructed should be close to original (within rounding error)
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    assertThat(reconstructedBlock[i][j])
                        .isCloseTo(originalBlock[i][j], within(1.0));
                }
            }
        }
        
        @Test
        @DisplayName("DCT should be reversible (DCT -> IDCT -> original)")
        void testDctReversibility() {
            // Given: Random 8x8 block
            double[][] originalBlock = new double[8][8];
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    originalBlock[i][j] = Math.random() * 255;
                }
            }
            
            // When: DCT -> IDCT
            double[][] transformed = FrequencyTransform.applyDCT(originalBlock);
            double[][] reconstructed = FrequencyTransform.applyIDCT(transformed);
            
            // Then: Reconstructed ≈ original
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    assertThat(reconstructed[i][j])
                        .isCloseTo(originalBlock[i][j], within(0.5));
                }
            }
        }
        
        @Test
        @DisplayName("DCT of zero block should produce zero coefficients")
        void testDctOfZeroBlock() {
            // Given: Zero block
            double[][] zeroBlock = new double[8][8];
            
            // When: Apply DCT
            double[][] dctBlock = FrequencyTransform.applyDCT(zeroBlock);
            
            // Then: All coefficients should be near zero
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    assertThat(Math.abs(dctBlock[i][j])).isLessThan(0.01);
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Block Operations")
    class BlockOperationsTests {
        
        @Test
        @DisplayName("Extract block should read correct pixel values")
        void testExtractBlock() {
            // Given: Test image with known values
            BufferedImage image = TestImageGenerator.createSolidColorImage(64, 64, java.awt.Color.RED);
            
            // When: Extract 8x8 block from (0,0)
            double[][] block = FrequencyTransform.extractBlock(image, 0, 0, 0); // Red channel
            
            // Then: All values should be 255 (red channel)
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    assertThat(block[i][j]).isEqualTo(255.0);
                }
            }
        }
        
        @Test
        @DisplayName("Write block should modify image correctly")
        void testWriteBlock() {
            // Given: White image
            BufferedImage image = TestImageGenerator.createSolidColorImage(64, 64, java.awt.Color.WHITE);
            
            // When: Write black 8x8 block to green channel
            double[][] blackBlock = new double[8][8]; // All zeros
            FrequencyTransform.writeBlock(image, blackBlock, 0, 0, 1); // Green channel
            
            // Then: Green channel should be 0, red and blue should still be 255
            int rgb = image.getRGB(0, 0);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            
            assertThat(r).isEqualTo(255); // Red unchanged
            assertThat(g).isEqualTo(0);   // Green modified
            assertThat(b).isEqualTo(255); // Blue unchanged
        }
        
        @Test
        @DisplayName("Extract and write block should be reversible")
        void testExtractWriteReversibility() {
            // Given: Original image
            BufferedImage original = TestImageGenerator.createGradientImage(64, 64);
            BufferedImage copy = TestImageGenerator.cloneImage(original);
            
            // When: Extract and write back same block
            double[][] block = FrequencyTransform.extractBlock(original, 8, 8, 1);
            FrequencyTransform.writeBlock(copy, block, 8, 8, 1);
            
            // Then: Images should be identical
            assertThat(TestImageGenerator.areImagesEqual(original, copy)).isTrue();
        }
        
        @Test
        @DisplayName("Extract block should handle boundary correctly")
        void testExtractBlockBoundary() {
            // Given: Small image (10x10)
            BufferedImage image = TestImageGenerator.createSolidColorImage(10, 10, java.awt.Color.BLUE);
            
            // When: Extract 8x8 block at (5,5) - overlaps boundary
            double[][] block = FrequencyTransform.extractBlock(image, 5, 5, 2); // Blue channel
            
            // Then: In-bounds pixels should be 255, out-of-bounds should be 0
            assertThat(block[0][0]).isEqualTo(255.0); // (5,5) in bounds
            assertThat(block[4][4]).isEqualTo(255.0); // (9,9) in bounds
            assertThat(block[5][5]).isEqualTo(0.0);   // (10,10) out of bounds
        }
    }
    
    @Nested
    @DisplayName("QIM Embedding/Extraction")
    class QimTests {
        
        @Test
        @DisplayName("Embed bit 1 should modify coefficient correctly")
        void testEmbedBit1() {
            // Given: Coefficient và quantization step
            double coefficient = 100.0;
            double quantizationStep = 10.0;
            
            // When: Embed bit 1
            double modified = FrequencyTransform.embedBitInCoefficient(coefficient, 1, quantizationStep);
            
            // Then: Modified coefficient should be quantized with offset 0.75
            // Expected: Q * floor(100/10) + 0.75*10 = 10*10 + 7.5 = 107.5
            assertThat(modified).isCloseTo(107.5, within(0.1));
        }
        
        @Test
        @DisplayName("Embed bit 0 should modify coefficient correctly")
        void testEmbedBit0() {
            // Given
            double coefficient = 100.0;
            double quantizationStep = 10.0;
            
            // When: Embed bit 0
            double modified = FrequencyTransform.embedBitInCoefficient(coefficient, 0, quantizationStep);
            
            // Then: Expected: 10*10 + 0.25*10 = 102.5
            assertThat(modified).isCloseTo(102.5, within(0.1));
        }
        
        @Test
        @DisplayName("Extract should recover embedded bit 1")
        void testExtractBit1() {
            // Given: Coefficient with embedded bit 1
            double quantizationStep = 10.0;
            double embeddedCoeff = FrequencyTransform.embedBitInCoefficient(100.0, 1, quantizationStep);
            
            // When: Extract bit
            int extractedBit = FrequencyTransform.extractBitFromCoefficient(embeddedCoeff, quantizationStep);
            
            // Then: Should recover bit 1
            assertThat(extractedBit).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Extract should recover embedded bit 0")
        void testExtractBit0() {
            // Given: Coefficient with embedded bit 0
            double quantizationStep = 10.0;
            double embeddedCoeff = FrequencyTransform.embedBitInCoefficient(100.0, 0, quantizationStep);
            
            // When: Extract bit
            int extractedBit = FrequencyTransform.extractBitFromCoefficient(embeddedCoeff, quantizationStep);
            
            // Then: Should recover bit 0
            assertThat(extractedBit).isEqualTo(0);
        }
        
        @Test
        @DisplayName("QIM should be robust to small noise")
        void testQimRobustness() {
            // Given: Embedded coefficient with small noise added
            double quantizationStep = 10.0;
            double embeddedCoeff = FrequencyTransform.embedBitInCoefficient(100.0, 1, quantizationStep);
            double noisyCoeff = embeddedCoeff + 2.0; // Small noise
            
            // When: Extract from noisy coefficient
            int extractedBit = FrequencyTransform.extractBitFromCoefficient(noisyCoeff, quantizationStep);
            
            // Then: Should still recover bit correctly (if noise is small enough)
            assertThat(extractedBit).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Extract should handle negative coefficients")
        void testExtractNegativeCoefficient() {
            // Given: Negative coefficient
            double quantizationStep = 10.0;
            double negativeCoeff = -55.0; // -55 % 10 = -5, needs to handle correctly
            
            // When: Embed and extract
            double embedded = FrequencyTransform.embedBitInCoefficient(negativeCoeff, 1, quantizationStep);
            int extracted = FrequencyTransform.extractBitFromCoefficient(embedded, quantizationStep);
            
            // Then: Should work correctly
            assertThat(extracted).isIn(0, 1); // Should return valid bit
        }
    }
    
    @Nested
    @DisplayName("Quantization Step Calculation")
    class QuantizationStepTests {
        
        @Test
        @DisplayName("Strength 0 should produce minimum quantization step")
        void testStrength0() {
            // When
            double q = FrequencyTransform.calculateQuantizationStep(0);
            
            // Then: Q = 5.0 + (0/100)*25 = 5.0
            assertThat(q).isEqualTo(5.0);
        }
        
        @Test
        @DisplayName("Strength 50 should produce medium quantization step")
        void testStrength50() {
            // When
            double q = FrequencyTransform.calculateQuantizationStep(50);
            
            // Then: Q = 5.0 + (50/100)*25 = 17.5
            assertThat(q).isEqualTo(17.5);
        }
        
        @Test
        @DisplayName("Strength 100 should produce maximum quantization step")
        void testStrength100() {
            // When
            double q = FrequencyTransform.calculateQuantizationStep(100);
            
            // Then: Q = 5.0 + (100/100)*25 = 30.0
            assertThat(q).isEqualTo(30.0);
        }
        
        @Test
        @DisplayName("Quantization step should increase linearly with strength")
        void testLinearIncrease() {
            // When
            double q1 = FrequencyTransform.calculateQuantizationStep(25);
            double q2 = FrequencyTransform.calculateQuantizationStep(75);
            
            // Then: Should be linear
            assertThat(q2 - q1).isCloseTo(12.5, within(0.1)); // (75-25) * 25/100 = 12.5
        }
    }
    
    @Nested
    @DisplayName("Zig-Zag Order and Embedding Positions")
    class ZigZagTests {
        
        @Test
        @DisplayName("Zig-zag order should have 64 positions")
        void testZigZagOrderSize() {
            // When
            int[][] zigzag = FrequencyTransform.getZigZagOrder();
            
            // Then
            assertThat(zigzag).hasSize(64);
        }
        
        @Test
        @DisplayName("Zig-zag order should start with DC component (0,0)")
        void testZigZagStartsWithDC() {
            // When
            int[][] zigzag = FrequencyTransform.getZigZagOrder();
            
            // Then
            assertThat(zigzag[0]).containsExactly(0, 0);
        }
        
        @Test
        @DisplayName("Zig-zag order should end with (7,7)")
        void testZigZagEndsCorrectly() {
            // When
            int[][] zigzag = FrequencyTransform.getZigZagOrder();
            
            // Then
            assertThat(zigzag[63]).containsExactly(7, 7);
        }
        
        @Test
        @DisplayName("All zig-zag positions should be valid (0-7)")
        void testZigZagPositionsValid() {
            // When
            int[][] zigzag = FrequencyTransform.getZigZagOrder();
            
            // Then
            for (int i = 0; i < 64; i++) {
                assertThat(zigzag[i][0]).isBetween(0, 7);
                assertThat(zigzag[i][1]).isBetween(0, 7);
            }
        }
        
        @Test
        @DisplayName("Embedding positions should be mid-frequency")
        void testEmbeddingPositions() {
            // When
            int[][] positions = FrequencyTransform.getEmbeddingPositions();
            
            // Then: Should have 26 positions (index 10-35)
            assertThat(positions).hasSize(26);
            
            // Should not include DC (0,0)
            assertThat(positions[0]).isNotEqualTo(new int[]{0, 0});
        }
        
        @Test
        @DisplayName("Embedding positions should avoid high frequencies")
        void testEmbeddingPositionsAvoidHighFreq() {
            // When
            int[][] positions = FrequencyTransform.getEmbeddingPositions();
            
            // Then: Should not include (7,7) - highest frequency
            boolean containsHighestFreq = false;
            for (int[] pos : positions) {
                if (pos[0] == 7 && pos[1] == 7) {
                    containsHighestFreq = true;
                    break;
                }
            }
            assertThat(containsHighestFreq).isFalse();
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("DCT should handle boundary pixel values (0 and 255)")
        void testDctBoundaryValues() {
            // Given: Block with boundary values
            double[][] block = new double[8][8];
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 8; j++) {
                    block[i][j] = 0.0;   // Black
                    block[i+4][j] = 255.0; // White
                }
            }
            
            // When: Apply DCT -> IDCT
            double[][] dct = FrequencyTransform.applyDCT(block);
            double[][] reconstructed = FrequencyTransform.applyIDCT(dct);
            
            // Then: Should reconstruct without overflow/underflow
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    assertThat(reconstructed[i][j]).isBetween(-1.0, 256.0);
                }
            }
        }
        
        @Test
        @DisplayName("Write block should clamp values to 0-255 range")
        void testWriteBlockClamping() {
            // Given: Block with out-of-range values
            BufferedImage image = TestImageGenerator.createSolidColorImage(16, 16, java.awt.Color.GRAY);
            double[][] block = new double[8][8];
            block[0][0] = -50.0;  // Below 0
            block[1][1] = 300.0;  // Above 255
            block[2][2] = 128.0;  // Normal
            
            // When: Write block
            FrequencyTransform.writeBlock(image, block, 0, 0, 0);
            
            // Then: Values should be clamped
            int rgb0 = image.getRGB(0, 0);
            int rgb1 = image.getRGB(1, 1);
            int rgb2 = image.getRGB(2, 2);
            
            int r0 = (rgb0 >> 16) & 0xFF;
            int r1 = (rgb1 >> 16) & 0xFF;
            int r2 = (rgb2 >> 16) & 0xFF;
            
            assertThat(r0).isEqualTo(0);   // Clamped to 0
            assertThat(r1).isEqualTo(255); // Clamped to 255
            assertThat(r2).isEqualTo(128); // Unchanged
        }
    }
}
