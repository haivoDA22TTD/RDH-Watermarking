package com.rdh.backend.validation;

import com.rdh.backend.exception.InvalidImageException;
import com.rdh.backend.exception.InsufficientCapacityException;
import org.springframework.stereotype.Component;

/**
 * Validator cho watermark text
 * Validate length, characters, capacity
 */
@Component
public class WatermarkTextValidator {
    
    // Constants
    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 64;
    private static final String DELIMITER = "###END###";
    
    // Printable ASCII range: 32-126
    private static final int MIN_ASCII = 32;
    private static final int MAX_ASCII = 126;
    
    /**
     * Validate watermark text
     * 
     * @param text Watermark text to validate
     * @throws InvalidImageException if validation fails
     */
    public void validateWatermarkText(String text) {
        // 1. Check not null or empty
        if (text == null || text.trim().isEmpty()) {
            throw new InvalidImageException("Watermark text is required and cannot be empty");
        }
        
        // 2. Check length (minimum)
        if (text.length() < MIN_LENGTH) {
            throw new InvalidImageException(
                String.format("Watermark text too short. Minimum: %d characters", MIN_LENGTH)
            );
        }
        
        // 3. Check length (maximum)
        if (text.length() > MAX_LENGTH) {
            throw new InvalidImageException(
                String.format("Watermark text too long. Maximum: %d characters, Actual: %d", 
                    MAX_LENGTH, text.length())
            );
        }
        
        // 4. Check for delimiter in text (reserved)
        if (text.contains(DELIMITER)) {
            throw new InvalidImageException(
                String.format("Watermark text cannot contain reserved delimiter: %s", DELIMITER)
            );
        }
        
        // 5. Validate characters (printable ASCII)
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < MIN_ASCII || c > MAX_ASCII) {
                throw new InvalidImageException(
                    String.format("Invalid character at position %d: '%c' (code: %d). Only printable ASCII characters (32-126) are allowed",
                        i, c, (int) c)
                );
            }
        }
        
        // 6. Warning for special characters that might cause issues
        if (containsProblematicCharacters(text)) {
            System.out.println(
                "Warning: Watermark contains special characters that might affect extraction accuracy"
            );
        }
    }
    
    /**
     * Validate watermark text with capacity check
     * 
     * @param text Watermark text
     * @param imageCapacity Available capacity in bits
     * @throws InvalidImageException if text invalid
     * @throws InsufficientCapacityException if text too large for image
     */
    public void validateWatermarkTextWithCapacity(String text, long imageCapacity) {
        // First validate text
        validateWatermarkText(text);
        
        // Calculate required bits
        long requiredBits = calculateRequiredBits(text);
        
        // Check capacity
        if (requiredBits > imageCapacity) {
            throw new InsufficientCapacityException(
                String.format("Insufficient image capacity. Required: %d bits, Available: %d bits. " +
                    "Try: 1) Use shorter watermark text, 2) Use larger image, or 3) Reduce image complexity",
                    requiredBits, imageCapacity)
            );
        }
        
        // Warning if using > 80% capacity
        double usagePercent = (requiredBits * 100.0) / imageCapacity;
        if (usagePercent > 80.0) {
            System.out.println(
                String.format("Warning: Using %.1f%% of image capacity. Embedding might be less robust.",
                    usagePercent)
            );
        }
    }
    
    /**
     * Calculate required bits for watermark text
     * Includes text + delimiter + overhead
     */
    public long calculateRequiredBits(String text) {
        if (text == null) {
            return 0;
        }
        
        // Text + delimiter
        String fullText = text + DELIMITER;
        
        // Each character = 8 bits
        return (long) fullText.length() * 8;
    }
    
    /**
     * Check if text contains problematic characters
     */
    private boolean containsProblematicCharacters(String text) {
        // Characters that might cause issues: control chars, extended ASCII
        for (char c : text.toCharArray()) {
            // Non-alphanumeric and not common punctuation
            if (!Character.isLetterOrDigit(c) && !isCommonPunctuation(c) && c != ' ') {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if character is common punctuation
     */
    private boolean isCommonPunctuation(char c) {
        String commonPunctuation = ".,;:!?-_()[]{}\"'/@#$%&*+=<>|\\~`";
        return commonPunctuation.indexOf(c) != -1;
    }
    
    /**
     * Sanitize watermark text (remove/replace problematic characters)
     */
    public String sanitizeWatermarkText(String text) {
        if (text == null) {
            return null;
        }
        
        StringBuilder sanitized = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= MIN_ASCII && c <= MAX_ASCII && !DELIMITER.contains(String.valueOf(c))) {
                sanitized.append(c);
            } else {
                sanitized.append('_'); // Replace invalid chars with underscore
            }
        }
        
        return sanitized.toString();
    }
    
    /**
     * Get maximum recommended text length for given capacity
     */
    public int getMaxRecommendedLength(long imageCapacity) {
        // Reserve bits for delimiter and overhead
        long delimiterBits = (long) DELIMITER.length() * 8;
        long overheadBits = 64; // Additional overhead
        
        long availableBits = imageCapacity - delimiterBits - overheadBits;
        
        // Each character = 8 bits
        int maxChars = (int) (availableBits / 8);
        
        // Cap at MAX_LENGTH
        return Math.min(maxChars, MAX_LENGTH);
    }
}
