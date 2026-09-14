package com.rdh.backend.util;

import org.apache.commons.math3.transform.DctNormalization;
import org.apache.commons.math3.transform.FastCosineTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.awt.image.BufferedImage;

/**
 * Utility class cho frequency domain transforms (DCT/IDCT)
 * Sử dụng trong DCT-based watermarking
 */
public class FrequencyTransform {
    
    /**
     * Apply 2D DCT (Discrete Cosine Transform) lên một block 8x8
     * 
     * @param block Block 8x8 pixels (giá trị 0-255)
     * @return DCT coefficients (frequency domain)
     */
    public static double[][] applyDCT(double[][] block) {
        int size = block.length;
        double[][] dctBlock = new double[size][size];
        
        // Step 1: DCT theo hàng (rows)
        for (int i = 0; i < size; i++) {
            dctBlock[i] = applyDCT1D(block[i]);
        }
        
        // Step 2: DCT theo cột (columns)
        double[][] result = new double[size][size];
        for (int j = 0; j < size; j++) {
            double[] column = new double[size];
            for (int i = 0; i < size; i++) {
                column[i] = dctBlock[i][j];
            }
            double[] dctColumn = applyDCT1D(column);
            for (int i = 0; i < size; i++) {
                result[i][j] = dctColumn[i];
            }
        }
        
        return result;
    }
    
    /**
     * Apply inverse 2D DCT (IDCT) để chuyển từ frequency domain về spatial domain
     * 
     * @param dctBlock DCT coefficients
     * @return Pixel values (spatial domain)
     */
    public static double[][] applyIDCT(double[][] dctBlock) {
        int size = dctBlock.length;
        double[][] idctBlock = new double[size][size];
        
        // Step 1: IDCT theo hàng (rows)
        for (int i = 0; i < size; i++) {
            idctBlock[i] = applyIDCT1D(dctBlock[i]);
        }
        
        // Step 2: IDCT theo cột (columns)
        double[][] result = new double[size][size];
        for (int j = 0; j < size; j++) {
            double[] column = new double[size];
            for (int i = 0; i < size; i++) {
                column[i] = idctBlock[i][j];
            }
            double[] idctColumn = applyIDCT1D(column);
            for (int i = 0; i < size; i++) {
                result[i][j] = idctColumn[i];
            }
        }
        
        return result;
    }
    
    /**
     * Apply 1D DCT sử dụng Apache Commons Math3
     * 
     * @param data Input data (8 elements)
     * @return DCT coefficients
     */
    private static double[] applyDCT1D(double[] data) {
        // Apache Commons Math3 FastCosineTransformer
        // Requires power of 2 size + 1, so we pad to 16 for size 8
        int paddedSize = getNextPowerOfTwoPlusOne(data.length);
        double[] padded = new double[paddedSize];
        System.arraycopy(data, 0, padded, 0, data.length);
        
        FastCosineTransformer transformer = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I);
        double[] transformed = transformer.transform(padded, TransformType.FORWARD);
        
        // Extract first 8 coefficients
        double[] result = new double[data.length];
        System.arraycopy(transformed, 0, result, 0, data.length);
        return result;
    }
    
    /**
     * Apply inverse 1D DCT
     * 
     * @param dctData DCT coefficients
     * @return Original data
     */
    private static double[] applyIDCT1D(double[] dctData) {
        int paddedSize = getNextPowerOfTwoPlusOne(dctData.length);
        double[] padded = new double[paddedSize];
        System.arraycopy(dctData, 0, padded, 0, dctData.length);
        
        FastCosineTransformer transformer = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I);
        double[] transformed = transformer.transform(padded, TransformType.INVERSE);
        
        double[] result = new double[dctData.length];
        System.arraycopy(transformed, 0, result, 0, dctData.length);
        return result;
    }
    
    /**
     * Get next power of 2 plus 1 (required by FastCosineTransformer)
     * For size 8 -> returns 17 (2^4 + 1)
     */
    private static int getNextPowerOfTwoPlusOne(int n) {
        int power = 1;
        while (power < n) {
            power *= 2;
        }
        return power + 1;
    }
    
    /**
     * Extract một block 8x8 từ ảnh tại vị trí (startX, startY)
     * 
     * @param image BufferedImage
     * @param startX Tọa độ x bắt đầu
     * @param startY Tọa độ y bắt đầu
     * @param channel Color channel (0=R, 1=G, 2=B)
     * @return Block 8x8 với giá trị 0-255
     */
    public static double[][] extractBlock(BufferedImage image, int startX, int startY, int channel) {
        double[][] block = new double[8][8];
        
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int x = startX + j;
                int y = startY + i;
                
                // Boundary check
                if (x >= image.getWidth() || y >= image.getHeight()) {
                    block[i][j] = 0;
                    continue;
                }
                
                int rgb = image.getRGB(x, y);
                int value;
                
                switch (channel) {
                    case 0: // Red
                        value = (rgb >> 16) & 0xFF;
                        break;
                    case 1: // Green
                        value = (rgb >> 8) & 0xFF;
                        break;
                    case 2: // Blue
                        value = rgb & 0xFF;
                        break;
                    default:
                        value = (rgb >> 8) & 0xFF; // Default to green
                }
                
                block[i][j] = value;
            }
        }
        
        return block;
    }
    
    /**
     * Ghi một block 8x8 trở lại vào ảnh tại vị trí (startX, startY)
     * 
     * @param image BufferedImage (sẽ bị modify)
     * @param block Block 8x8 với giá trị 0-255
     * @param startX Tọa độ x bắt đầu
     * @param startY Tọa độ y bắt đầu
     * @param channel Color channel (0=R, 1=G, 2=B)
     */
    public static void writeBlock(BufferedImage image, double[][] block, int startX, int startY, int channel) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                int x = startX + j;
                int y = startY + i;
                
                // Boundary check
                if (x >= image.getWidth() || y >= image.getHeight()) {
                    continue;
                }
                
                // Clamp value to 0-255
                int value = (int) Math.round(block[i][j]);
                value = Math.max(0, Math.min(255, value));
                
                // Get current RGB
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Update specific channel
                switch (channel) {
                    case 0: // Red
                        r = value;
                        break;
                    case 1: // Green
                        g = value;
                        break;
                    case 2: // Blue
                        b = value;
                        break;
                }
                
                // Write back
                int newRgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, newRgb);
            }
        }
    }
    
    /**
     * Modify một DCT coefficient để embed watermark bit
     * Sử dụng Quantization Index Modulation (QIM)
     * 
     * @param coefficient DCT coefficient gốc
     * @param bit Watermark bit (0 hoặc 1)
     * @param quantizationStep Quantization step (phụ thuộc strength)
     * @return Modified coefficient
     */
    public static double embedBitInCoefficient(double coefficient, int bit, double quantizationStep) {
        // QIM algorithm:
        // If bit = 1: coeff = Q * floor(coeff/Q) + 0.75*Q
        // If bit = 0: coeff = Q * floor(coeff/Q) + 0.25*Q
        
        double quotient = Math.floor(coefficient / quantizationStep);
        double offset = (bit == 1) ? 0.75 : 0.25;
        
        return quantizationStep * quotient + offset * quantizationStep;
    }
    
    /**
     * Extract watermark bit từ DCT coefficient
     * 
     * @param coefficient DCT coefficient
     * @param quantizationStep Quantization step (phải giống lúc embed)
     * @return Extracted bit (0 hoặc 1)
     */
    public static int extractBitFromCoefficient(double coefficient, double quantizationStep) {
        // Extract bit:
        // remainder = coeff mod Q
        // If remainder > 0.5*Q: bit = 1
        // Else: bit = 0
        
        double remainder = coefficient % quantizationStep;
        return (remainder > 0.5 * quantizationStep) ? 1 : 0;
    }
    
    /**
     * Calculate quantization step dựa vào strength parameter
     * 
     * @param strength Strength (0-100)
     * @return Quantization step (higher = stronger watermark)
     */
    public static double calculateQuantizationStep(int strength) {
        // Strength 0 -> Q = 5 (weak, PSNR cao)
        // Strength 50 -> Q = 15 (balanced)
        // Strength 100 -> Q = 30 (strong, PSNR thấp)
        
        return 5.0 + (strength / 100.0) * 25.0;
    }
    
    /**
     * Get zig-zag scan order cho DCT coefficients
     * Trả về list (row, col) positions theo thứ tự zig-zag
     * 
     * @return Array of [row, col] positions
     */
    public static int[][] getZigZagOrder() {
        // Zig-zag order cho 8x8 block
        // DC (0,0) ở đầu, AC coefficients theo thứ tự zig-zag
        // Chúng ta sẽ embed vào mid-frequency (vị trí 10-40)
        
        return new int[][] {
            {0, 0}, // DC component
            {0, 1}, {1, 0},
            {2, 0}, {1, 1}, {0, 2},
            {0, 3}, {1, 2}, {2, 1}, {3, 0},
            {4, 0}, {3, 1}, {2, 2}, {1, 3}, {0, 4},
            {0, 5}, {1, 4}, {2, 3}, {3, 2}, {4, 1}, {5, 0},
            {6, 0}, {5, 1}, {4, 2}, {3, 3}, {2, 4}, {1, 5}, {0, 6},
            {0, 7}, {1, 6}, {2, 5}, {3, 4}, {4, 3}, {5, 2}, {6, 1}, {7, 0},
            {7, 1}, {6, 2}, {5, 3}, {4, 4}, {3, 5}, {2, 6}, {1, 7},
            {2, 7}, {3, 6}, {4, 5}, {5, 4}, {6, 3}, {7, 2},
            {7, 3}, {6, 4}, {5, 5}, {4, 6}, {3, 7},
            {4, 7}, {5, 6}, {6, 5}, {7, 4},
            {7, 5}, {6, 6}, {5, 7},
            {6, 7}, {7, 6},
            {7, 7}
        };
    }
    
    /**
     * Get positions để embed watermark (mid-frequency coefficients)
     * Bỏ qua DC và low-frequency, cũng như high-frequency
     * 
     * @return Array of [row, col] positions for embedding
     */
    public static int[][] getEmbeddingPositions() {
        int[][] zigzag = getZigZagOrder();
        
        // Chọn mid-frequency: vị trí 10-35 trong zig-zag order
        // Đủ robust mà không ảnh hưởng nhiều đến chất lượng ảnh
        int startIdx = 10;
        int endIdx = 35;
        int count = endIdx - startIdx + 1;
        
        int[][] positions = new int[count][2];
        for (int i = 0; i < count; i++) {
            positions[i] = zigzag[startIdx + i];
        }
        
        return positions;
    }
}
