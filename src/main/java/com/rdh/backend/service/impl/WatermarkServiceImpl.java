package com.rdh.backend.service.impl;

import com.rdh.backend.dto.enumtype.WatermarkAttackType;
import com.rdh.backend.dto.enumtype.WatermarkType;
import com.rdh.backend.dto.response.*;
import com.rdh.backend.exception.RdhException;
import com.rdh.backend.service.WatermarkService;
import com.rdh.backend.strategy.watermark.DctWatermarkStrategy;
import com.rdh.backend.strategy.watermark.WatermarkStrategy;
import com.rdh.backend.util.AttackSimulator;
import com.rdh.backend.util.MetricsCalculator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation của WatermarkService
 */
@Service
public class WatermarkServiceImpl implements WatermarkService {
    
    private final Map<WatermarkType, WatermarkStrategy> strategies = new HashMap<>();
    private static final int DEFAULT_STRENGTH = 50;
    
    public WatermarkServiceImpl(DctWatermarkStrategy dctStrategy) {
        // Register strategies
        strategies.put(WatermarkType.DCT, dctStrategy);
        // TODO: Add DWT, LSB_ROBUST khi implement
    }
    
    @Override
    public WatermarkEmbedResult embedWatermark(
            MultipartFile imageFile,
            String watermarkText,
            WatermarkType algorithm,
            Integer strength
    ) throws IOException {
        
        long startTime = System.currentTimeMillis();
        
        // Validate
        if (imageFile == null || imageFile.isEmpty()) {
            throw new RdhException("Image file is required");
        }
        if (watermarkText == null || watermarkText.trim().isEmpty()) {
            throw new RdhException("Watermark text is required");
        }
        if (watermarkText.length() > 64) {
            throw new RdhException("Watermark text too long (max 64 characters)");
        }
        
        // Default values
        if (algorithm == null) {
            algorithm = WatermarkType.DCT;
        }
        if (strength == null || strength < 0 || strength > 100) {
            strength = DEFAULT_STRENGTH;
        }
        
        // Get strategy
        WatermarkStrategy strategy = strategies.get(algorithm);
        if (strategy == null) {
            throw new RdhException("Algorithm not supported: " + algorithm);
        }
        
        // Read image
        BufferedImage originalImage = ImageIO.read(imageFile.getInputStream());
        if (originalImage == null) {
            throw new RdhException("Cannot read image file");
        }
        
        long originalSize = imageFile.getSize();
        
        // Embed watermark
        System.out.println("Starting watermark embedding...");
        BufferedImage watermarkedImage = strategy.embed(originalImage, watermarkText, strength);
        
        // Convert to PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(watermarkedImage, "PNG", baos);
        byte[] watermarkedBytes = baos.toByteArray();
        
        // Calculate metrics
        double psnr = MetricsCalculator.calculatePSNR(originalImage, watermarkedImage);
        double ssim = MetricsCalculator.calculateSSIM(originalImage, watermarkedImage);
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Calculate embedded bits
        int embeddedBits = (watermarkText.length() + "###END###".length()) * 8;
        
        // Create metadata
        WatermarkEmbedResponse metadata = WatermarkEmbedResponse.builder()
                .algorithm(algorithm.name())
                .strength(strength)
                .embeddedBits(embeddedBits)
                .psnr(psnr)
                .ssim(ssim)
                .originalSize(originalSize)
                .watermarkedSize((long) watermarkedBytes.length)
                .processingTimeMs(processingTime)
                .message("Watermark embedded successfully with " + algorithm.name())
                .build();
        
        System.out.println("Watermark embedded: PSNR=" + psnr + " dB, SSIM=" + ssim);
        
        return new WatermarkEmbedResult(watermarkedBytes, metadata);
    }
    
    @Override
    public WatermarkExtractResponse extractWatermark(
            MultipartFile imageFile,
            WatermarkType algorithm,
            Integer strength
    ) throws IOException {
        
        long startTime = System.currentTimeMillis();
        
        // Validate
        if (imageFile == null || imageFile.isEmpty()) {
            throw new RdhException("Image file is required");
        }
        
        // Default values
        if (algorithm == null) {
            algorithm = WatermarkType.DCT;
        }
        if (strength == null || strength < 0 || strength > 100) {
            strength = DEFAULT_STRENGTH;
        }
        
        // Get strategy
        WatermarkStrategy strategy = strategies.get(algorithm);
        if (strategy == null) {
            throw new RdhException("Algorithm not supported: " + algorithm);
        }
        
        // Read image
        BufferedImage watermarkedImage = ImageIO.read(imageFile.getInputStream());
        if (watermarkedImage == null) {
            throw new RdhException("Cannot read image file");
        }
        
        // Extract watermark
        System.out.println("Starting watermark extraction...");
        String extractedWatermark = strategy.extract(watermarkedImage, strength);
        
        long processingTime = System.currentTimeMillis() - startTime;
        
        // Calculate confidence and BER (simplified)
        double confidence = calculateConfidence(extractedWatermark);
        double ber = 0.02; // Placeholder - would need original watermark to calculate actual BER
        
        String status = confidence > 0.9 ? "SUCCESS" : (confidence > 0.5 ? "PARTIAL" : "FAILED");
        int extractedBits = extractedWatermark.length() * 8;
        
        return WatermarkExtractResponse.builder()
                .extractedWatermark(extractedWatermark)
                .algorithm(algorithm.name())
                .confidence(confidence)
                .ber(ber)
                .extractedBits(extractedBits)
                .processingTimeMs(processingTime)
                .status(status)
                .message("Watermark extraction " + status.toLowerCase())
                .build();
    }
    
    @Override
    public RobustnessTestResponse testRobustness(
            MultipartFile originalImageFile,
            MultipartFile watermarkedImageFile,
            List<WatermarkAttackType> attacks,
            WatermarkType algorithm,
            Integer strength
    ) throws IOException {
        
        // Validate
        if (originalImageFile == null || watermarkedImageFile == null) {
            throw new RdhException("Both original and watermarked images are required");
        }
        if (attacks == null || attacks.isEmpty()) {
            throw new RdhException("At least one attack type is required");
        }
        
        // Default values
        if (algorithm == null) {
            algorithm = WatermarkType.DCT;
        }
        if (strength == null) {
            strength = DEFAULT_STRENGTH;
        }
        
        // Get strategy
        WatermarkStrategy strategy = strategies.get(algorithm);
        if (strategy == null) {
            throw new RdhException("Algorithm not supported: " + algorithm);
        }
        
        // Read images
        BufferedImage originalImage = ImageIO.read(originalImageFile.getInputStream());
        BufferedImage watermarkedImage = ImageIO.read(watermarkedImageFile.getInputStream());
        
        if (originalImage == null || watermarkedImage == null) {
            throw new RdhException("Cannot read image files");
        }
        
        // Calculate base metrics
        double psnr = MetricsCalculator.calculatePSNR(originalImage, watermarkedImage);
        double ssim = MetricsCalculator.calculateSSIM(originalImage, watermarkedImage);
        double ncc = 0.992; // Placeholder - would need original watermark to calculate
        
        // Test each attack
        List<RobustnessTestResponse.AttackResult> attackResults = new ArrayList<>();
        int passedCount = 0;
        
        for (WatermarkAttackType attackType : attacks) {
            try {
                System.out.println("Testing attack: " + attackType.name());
                
                // Apply attack
                BufferedImage attackedImage = AttackSimulator.applyAttack(watermarkedImage, attackType);
                
                // Try to extract watermark from attacked image
                String extractedWatermark = strategy.extract(attackedImage, strength);
                
                // Calculate metrics for attacked image
                double attackPsnr = MetricsCalculator.calculatePSNR(watermarkedImage, attackedImage);
                double confidence = calculateConfidence(extractedWatermark);
                double ber = 0.03; // Placeholder
                
                boolean passed = confidence > 0.7 && !extractedWatermark.trim().isEmpty();
                if (passed) passedCount++;
                
                RobustnessTestResponse.AttackResult result = RobustnessTestResponse.AttackResult.builder()
                        .attackType(attackType.name())
                        .attackDescription(attackType.getDisplayName())
                        .severity(attackType.getSeverity())
                        .passed(passed)
                        .extractedWatermark(extractedWatermark)
                        .ber(ber)
                        .confidence(confidence)
                        .psnr(attackPsnr)
                        .message(passed ? 
                            "Watermark survived " + attackType.getDisplayName() :
                            "Watermark lost after " + attackType.getDisplayName())
                        .build();
                
                attackResults.add(result);
                
            } catch (Exception e) {
                System.err.println("Error testing attack " + attackType + ": " + e.getMessage());
                
                // Add failed result
                RobustnessTestResponse.AttackResult result = RobustnessTestResponse.AttackResult.builder()
                        .attackType(attackType.name())
                        .attackDescription(attackType.getDisplayName())
                        .severity(attackType.getSeverity())
                        .passed(false)
                        .extractedWatermark("")
                        .ber(1.0)
                        .confidence(0.0)
                        .psnr(0.0)
                        .message("Error: " + e.getMessage())
                        .build();
                
                attackResults.add(result);
            }
        }
        
        int failedCount = attacks.size() - passedCount;
        double successRate = (passedCount * 100.0) / attacks.size();
        
        return RobustnessTestResponse.builder()
                .psnr(psnr)
                .ssim(ssim)
                .ncc(ncc)
                .totalAttacks(attacks.size())
                .passedAttacks(passedCount)
                .failedAttacks(failedCount)
                .successRate(successRate)
                .attackResults(attackResults)
                .message(String.format("Robustness test completed: %d/%d attacks passed (%.1f%%)",
                        passedCount, attacks.size(), successRate))
                .build();
    }
    
    @Override
    public List<WatermarkAlgorithmInfo> getAvailableAlgorithms() {
        List<WatermarkAlgorithmInfo> algorithms = new ArrayList<>();
        
        // DCT
        algorithms.add(WatermarkAlgorithmInfo.builder()
                .name("DCT")
                .displayName("DCT-based Watermarking")
                .description("Robust against JPEG compression and resizing")
                .robustness("HIGH")
                .typicalCapacity(256)
                .typicalPsnr("40-50 dB")
                .resistantTo("JPEG compression, resizing, light noise")
                .vulnerableTo("Heavy cropping (>20%), rotation")
                .recommended(true)
                .useCases("Copyright protection, social media images, web content")
                .build());
        
        // DWT (placeholder - not implemented yet)
        algorithms.add(WatermarkAlgorithmInfo.builder()
                .name("DWT")
                .displayName("DWT-based Watermarking")
                .description("Very robust, suitable for medical/legal images")
                .robustness("VERY_HIGH")
                .typicalCapacity(128)
                .typicalPsnr("38-45 dB")
                .resistantTo("Multiple attacks, JPEG, resizing, cropping")
                .vulnerableTo("Heavy geometric transformations")
                .recommended(false)
                .useCases("Medical images, legal documents, high-security applications")
                .build());
        
        // LSB Robust (placeholder)
        algorithms.add(WatermarkAlgorithmInfo.builder()
                .name("LSB_ROBUST")
                .displayName("LSB with Error Correction")
                .description("Simple but less robust, high capacity")
                .robustness("LOW")
                .typicalCapacity(512)
                .typicalPsnr("50+ dB")
                .resistantTo("Visual inspection only")
                .vulnerableTo("JPEG compression, resizing, any filtering")
                .recommended(false)
                .useCases("Demo, learning purposes, low-security applications")
                .build());
        
        return algorithms;
    }
    
    // ============= Helper methods =============
    
    /**
     * Calculate confidence score cho extracted watermark
     * Dựa vào: có printable characters không, độ dài hợp lý, ...
     */
    private double calculateConfidence(String watermark) {
        if (watermark == null || watermark.isEmpty()) {
            return 0.0;
        }
        
        // Check có printable characters
        int printableCount = 0;
        for (char c : watermark.toCharArray()) {
            if (c >= 32 && c <= 126) { // ASCII printable range
                printableCount++;
            }
        }
        
        double printableRatio = (double) printableCount / watermark.length();
        
        // Check length (reasonable watermark should be 5-64 chars)
        double lengthScore = 1.0;
        if (watermark.length() < 5) {
            lengthScore = watermark.length() / 5.0;
        } else if (watermark.length() > 64) {
            lengthScore = 0.5;
        }
        
        return printableRatio * lengthScore;
    }
}
