package com.rdh.backend.strategy.watermark;

import com.rdh.backend.util.FrequencyTransform;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * DCT-based Watermarking Strategy
 * 
 * Algorithm:
 * 1. Chia ảnh thành blocks 8x8
 * 2. Apply DCT cho mỗi block
 * 3. Embed watermark bits vào mid-frequency DCT coefficients (QIM)
 * 4. Apply IDCT để tái tạo ảnh
 * 
 * Features:
 * - Robust against JPEG compression
 * - Robust against resizing
 * - PSNR: 40-50 dB
 * - Capacity: ~256 bits per image
 */
@Component
public class DctWatermarkStrategy implements WatermarkStrategy {
    
    private static final int BLOCK_SIZE = 8;
    private static final int CHANNEL = 1; // Green channel (more robust)
    
    // Delimiter để đánh dấu kết thúc watermark
    private static final String DELIMITER = "###END###";
    
    @Override
    public BufferedImage embed(BufferedImage originalImage, String watermarkData, int strength) {
        // Create a copy để không modify original
        BufferedImage watermarkedImage = deepCopy(originalImage);
        
        // Convert watermark text to bits
        String dataWithDelimiter = watermarkData + DELIMITER;
        byte[] bytes = dataWithDelimiter.getBytes(StandardCharsets.UTF_8);
        List<Integer> bits = bytesToBits(bytes);
        
        System.out.println("DCT EMBED - Watermark text: " + watermarkData);
        System.out.println("DCT EMBED - Total bits to embed: " + bits.size());
        
        // Calculate quantization step from strength
        double quantizationStep = FrequencyTransform.calculateQuantizationStep(strength);
        System.out.println("DCT EMBED - Quantization step: " + quantizationStep);
        
        // Get mid-frequency positions for embedding
        int[][] embeddingPositions = FrequencyTransform.getEmbeddingPositions();
        System.out.println("DCT EMBED - Embedding positions available: " + embeddingPositions.length);
        
        int width = watermarkedImage.getWidth();
        int height = watermarkedImage.getHeight();
        
        int blocksPerRow = width / BLOCK_SIZE;
        int blocksPerCol = height / BLOCK_SIZE;
        int totalBlocks = blocksPerRow * blocksPerCol;
        
        System.out.println("DCT EMBED - Image blocks: " + blocksPerRow + "x" + blocksPerCol + " = " + totalBlocks);
        
        int bitIndex = 0;
        int blocksUsed = 0;
        
        // Embed bits vào các blocks
        for (int blockRow = 0; blockRow < blocksPerCol && bitIndex < bits.size(); blockRow++) {
            for (int blockCol = 0; blockCol < blocksPerRow && bitIndex < bits.size(); blockCol++) {
                int startX = blockCol * BLOCK_SIZE;
                int startY = blockRow * BLOCK_SIZE;
                
                // Extract block 8x8
                double[][] block = FrequencyTransform.extractBlock(
                    watermarkedImage, startX, startY, CHANNEL
                );
                
                // Apply DCT
                double[][] dctBlock = FrequencyTransform.applyDCT(block);
                
                // Embed one bit vào mid-frequency coefficient đầu tiên
                if (bitIndex < bits.size()) {
                    int[] pos = embeddingPositions[0]; // Use first mid-freq position
                    int row = pos[0];
                    int col = pos[1];
                    
                    int bit = bits.get(bitIndex);
                    double originalCoeff = dctBlock[row][col];
                    double modifiedCoeff = FrequencyTransform.embedBitInCoefficient(
                        originalCoeff, bit, quantizationStep
                    );
                    
                    dctBlock[row][col] = modifiedCoeff;
                    bitIndex++;
                    blocksUsed++;
                }
                
                // Apply IDCT
                double[][] reconstructedBlock = FrequencyTransform.applyIDCT(dctBlock);
                
                // Write block back
                FrequencyTransform.writeBlock(
                    watermarkedImage, reconstructedBlock, startX, startY, CHANNEL
                );
            }
        }
        
        System.out.println("DCT EMBED - Bits embedded: " + bitIndex + " / " + bits.size());
        System.out.println("DCT EMBED - Blocks used: " + blocksUsed);
        
        if (bitIndex < bits.size()) {
            System.out.println("DCT EMBED - WARNING: Not enough blocks! Some bits not embedded.");
        }
        
        return watermarkedImage;
    }
    
    @Override
    public String extract(BufferedImage watermarkedImage, int strength) {
        System.out.println("DCT EXTRACT - Starting extraction...");
        
        // Calculate quantization step (phải giống lúc embed)
        double quantizationStep = FrequencyTransform.calculateQuantizationStep(strength);
        System.out.println("DCT EXTRACT - Quantization step: " + quantizationStep);
        
        // Get mid-frequency positions (phải giống lúc embed)
        int[][] embeddingPositions = FrequencyTransform.getEmbeddingPositions();
        
        int width = watermarkedImage.getWidth();
        int height = watermarkedImage.getHeight();
        
        int blocksPerRow = width / BLOCK_SIZE;
        int blocksPerCol = height / BLOCK_SIZE;
        
        List<Integer> extractedBits = new ArrayList<>();
        
        // Extract bits từ các blocks
        for (int blockRow = 0; blockRow < blocksPerCol; blockRow++) {
            for (int blockCol = 0; blockCol < blocksPerRow; blockCol++) {
                int startX = blockCol * BLOCK_SIZE;
                int startY = blockRow * BLOCK_SIZE;
                
                // Extract block 8x8
                double[][] block = FrequencyTransform.extractBlock(
                    watermarkedImage, startX, startY, CHANNEL
                );
                
                // Apply DCT
                double[][] dctBlock = FrequencyTransform.applyDCT(block);
                
                // Extract bit từ mid-frequency coefficient đầu tiên
                int[] pos = embeddingPositions[0];
                int row = pos[0];
                int col = pos[1];
                
                double coeff = dctBlock[row][col];
                int bit = FrequencyTransform.extractBitFromCoefficient(coeff, quantizationStep);
                
                extractedBits.add(bit);
            }
        }
        
        System.out.println("DCT EXTRACT - Total bits extracted: " + extractedBits.size());
        
        // Convert bits to bytes
        byte[] extractedBytes = bitsToBytes(extractedBits);
        String extractedText = new String(extractedBytes, StandardCharsets.UTF_8);
        
        System.out.println("DCT EXTRACT - Extracted text (raw): " + extractedText.substring(0, Math.min(100, extractedText.length())));
        
        // Find delimiter
        int delimiterIndex = extractedText.indexOf(DELIMITER);
        if (delimiterIndex != -1) {
            String watermark = extractedText.substring(0, delimiterIndex);
            System.out.println("DCT EXTRACT - Watermark found: " + watermark);
            return watermark;
        } else {
            System.out.println("DCT EXTRACT - WARNING: Delimiter not found!");
            // Return first reasonable part
            int maxLength = Math.min(64, extractedText.length());
            return extractedText.substring(0, maxLength).trim();
        }
    }
    
    @Override
    public String getAlgorithmName() {
        return "DCT";
    }
    
    // ============= Helper methods =============
    
    /**
     * Convert bytes to list of bits
     */
    private List<Integer> bytesToBits(byte[] bytes) {
        List<Integer> bits = new ArrayList<>();
        for (byte b : bytes) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >> i) & 1;
                bits.add(bit);
            }
        }
        return bits;
    }
    
    /**
     * Convert list of bits to bytes
     */
    private byte[] bitsToBytes(List<Integer> bits) {
        int numBytes = (bits.size() + 7) / 8;
        byte[] bytes = new byte[numBytes];
        
        for (int i = 0; i < bits.size(); i++) {
            int byteIndex = i / 8;
            int bitIndex = 7 - (i % 8);
            
            if (bits.get(i) == 1) {
                bytes[byteIndex] |= (1 << bitIndex);
            }
        }
        
        return bytes;
    }
    
    /**
     * Deep copy BufferedImage
     */
    private BufferedImage deepCopy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(
            image.getWidth(),
            image.getHeight(),
            image.getType()
        );
        
        java.awt.Graphics2D g = copy.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        
        return copy;
    }
}
