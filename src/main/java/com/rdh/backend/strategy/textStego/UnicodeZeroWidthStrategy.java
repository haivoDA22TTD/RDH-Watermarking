package com.rdh.backend.strategy.textStego;

import com.rdh.backend.exception.WatermarkNotFoundException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Unicode Zero-Width Character Steganography Strategy.
 * 
 * Algorithm:
 * - Converts secret data to binary
 * - Inserts zero-width characters between normal characters
 * - ZERO WIDTH SPACE (U+200B) = bit 0
 * - ZERO WIDTH JOINER (U+200D) = bit 1
 * - End marker: ZERO WIDTH NON-JOINER (U+200C)
 * 
 * Advantages:
 * - Works with ANY text format (txt, md, json, code, html, etc.)
 * - Completely invisible in most text editors
 * - Hard to detect without specific analysis
 * - Good capacity (1 bit per character position)
 * 
 * Example:
 * Original: "Hello World"
 * Stego:    "H​e​‍l​l​o​ ​W​o​r​l​d" (contains invisible ZW characters)
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-08
 */
@Component
public class UnicodeZeroWidthStrategy implements TextStegoStrategy {
    
    // Zero-width Unicode characters (invisible)
    private static final char ZERO_WIDTH_SPACE = '\u200B';      // Bit 0
    private static final char ZERO_WIDTH_JOINER = '\u200D';     // Bit 1
    private static final char ZERO_WIDTH_NON_JOINER = '\u200C'; // End marker
    
    // Delimiter to mark end of secret data
    private static final String END_MARKER = String.valueOf(ZERO_WIDTH_NON_JOINER);
    
    @Override
    public String embed(String coverText, String secretData) {
        // Validation
        if (coverText == null || coverText.isEmpty()) {
            throw new IllegalArgumentException("Cover text cannot be null or empty");
        }
        
        if (secretData == null || secretData.isEmpty()) {
            throw new IllegalArgumentException("Secret data cannot be null or empty");
        }
        
        // Check capacity
        long capacity = calculateCapacity(coverText);
        long requiredBits = (long) secretData.length() * 8;
        
        if (requiredBits > capacity) {
            throw new IllegalArgumentException(
                String.format("Insufficient capacity. Required: %d bits, Available: %d bits. " +
                    "Try shorter secret data or longer cover text.",
                    requiredBits, capacity)
            );
        }
        
        // Convert secret data to binary string
        String binaryData = textToBinary(secretData);
        
        // Build stego text by inserting ZW characters
        StringBuilder stegoText = new StringBuilder();
        int binaryIndex = 0;
        
        for (int i = 0; i < coverText.length() && binaryIndex < binaryData.length(); i++) {
            char currentChar = coverText.charAt(i);
            stegoText.append(currentChar);
            
            // Don't insert ZW chars after whitespace or control characters
            if (!Character.isWhitespace(currentChar) && !Character.isISOControl(currentChar)) {
                // Insert ZW character based on bit value
                char bit = binaryData.charAt(binaryIndex);
                if (bit == '0') {
                    stegoText.append(ZERO_WIDTH_SPACE);
                } else {
                    stegoText.append(ZERO_WIDTH_JOINER);
                }
                binaryIndex++;
            }
        }
        
        // Add end marker
        stegoText.append(END_MARKER);
        
        // Append remaining cover text if any
        if (binaryIndex >= binaryData.length()) {
            int lastCharIndex = stegoText.length();
            // Count visible chars used
            int charsUsed = 0;
            for (int i = 0; i < coverText.length(); i++) {
                char c = coverText.charAt(i);
                if (!Character.isWhitespace(c) && !Character.isISOControl(c)) {
                    charsUsed++;
                    if (charsUsed >= binaryData.length()) {
                        stegoText.append(coverText.substring(i + 1));
                        break;
                    }
                }
            }
        }
        
        return stegoText.toString();
    }
    
    @Override
    public String extract(String stegoText) {
        // Validation
        if (stegoText == null || stegoText.isEmpty()) {
            throw new IllegalArgumentException("Stego text cannot be null or empty");
        }
        
        // Extract ZW characters
        StringBuilder binaryData = new StringBuilder();
        boolean foundEndMarker = false;
        
        for (int i = 0; i < stegoText.length(); i++) {
            char c = stegoText.charAt(i);
            
            if (c == ZERO_WIDTH_SPACE) {
                binaryData.append('0');
            } else if (c == ZERO_WIDTH_JOINER) {
                binaryData.append('1');
            } else if (c == ZERO_WIDTH_NON_JOINER) {
                foundEndMarker = true;
                break;
            }
        }
        
        // Check if any hidden data found
        if (binaryData.length() == 0) {
            throw new WatermarkNotFoundException(
                "No hidden data found in text. Text may not contain embedded data or data may be corrupted."
            );
        }
        
        // Convert binary to text
        try {
            String secretData = binaryToText(binaryData.toString());
            
            if (secretData.isEmpty()) {
                throw new WatermarkNotFoundException("Extracted data is empty or corrupted");
            }
            
            return secretData;
            
        } catch (IllegalArgumentException e) {
            throw new WatermarkNotFoundException(
                "Failed to extract hidden data. Data may be corrupted: " + e.getMessage()
            );
        }
    }
    
    @Override
    public boolean hasHiddenData(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        // Check for presence of ZW characters
        for (char c : text.toCharArray()) {
            if (c == ZERO_WIDTH_SPACE || 
                c == ZERO_WIDTH_JOINER || 
                c == ZERO_WIDTH_NON_JOINER) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public long calculateCapacity(String coverText) {
        if (coverText == null || coverText.isEmpty()) {
            return 0;
        }
        
        // Count non-whitespace, non-control characters
        // Each can hide 1 bit
        long capacity = 0;
        for (char c : coverText.toCharArray()) {
            if (!Character.isWhitespace(c) && !Character.isISOControl(c)) {
                capacity++;
            }
        }
        
        // Convert bits to bytes (8 bits per byte)
        return capacity / 8;
    }
    
    @Override
    public String getAlgorithmName() {
        return "Unicode Zero-Width Characters";
    }
    
    /**
     * Convert text to binary string.
     * Each character → 8-bit binary.
     * 
     * @param text Input text
     * @return Binary string (e.g., "01001000" for 'H')
     */
    private String textToBinary(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        StringBuilder binary = new StringBuilder();
        
        for (byte b : bytes) {
            // Convert byte to 8-bit binary string
            String binaryByte = String.format("%8s", Integer.toBinaryString(b & 0xFF))
                .replace(' ', '0');
            binary.append(binaryByte);
        }
        
        return binary.toString();
    }
    
    /**
     * Convert binary string to text.
     * Each 8 bits → 1 character.
     * 
     * @param binaryString Binary string
     * @return Decoded text
     * @throws IllegalArgumentException if binary string is invalid
     */
    private String binaryToText(String binaryString) {
        // Validate binary string
        if (binaryString.length() % 8 != 0) {
            // Pad with zeros to make it multiple of 8
            int padding = 8 - (binaryString.length() % 8);
            binaryString = binaryString + "0".repeat(padding);
        }
        
        // Convert each 8 bits to a byte
        byte[] bytes = new byte[binaryString.length() / 8];
        
        for (int i = 0; i < bytes.length; i++) {
            String byteString = binaryString.substring(i * 8, (i + 1) * 8);
            bytes[i] = (byte) Integer.parseInt(byteString, 2);
        }
        
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    /**
     * Remove all zero-width characters from text (clean stego text).
     * 
     * @param text Text with ZW characters
     * @return Clean text without ZW characters
     */
    public String removeHiddenData(String text) {
        if (text == null) {
            return null;
        }
        
        return text.replace(String.valueOf(ZERO_WIDTH_SPACE), "")
                   .replace(String.valueOf(ZERO_WIDTH_JOINER), "")
                   .replace(String.valueOf(ZERO_WIDTH_NON_JOINER), "");
    }
    
    /**
     * Get statistics about hidden data in text.
     * 
     * @param stegoText Stego text
     * @return Statistics string
     */
    public String getStatistics(String stegoText) {
        if (stegoText == null || stegoText.isEmpty()) {
            return "No text provided";
        }
        
        int zwSpaceCount = 0;
        int zwJoinerCount = 0;
        int zwNonJoinerCount = 0;
        
        for (char c : stegoText.toCharArray()) {
            if (c == ZERO_WIDTH_SPACE) zwSpaceCount++;
            else if (c == ZERO_WIDTH_JOINER) zwJoinerCount++;
            else if (c == ZERO_WIDTH_NON_JOINER) zwNonJoinerCount++;
        }
        
        int totalZwChars = zwSpaceCount + zwJoinerCount + zwNonJoinerCount;
        int estimatedBytes = (zwSpaceCount + zwJoinerCount) / 8;
        
        return String.format(
            "ZW Characters: %d total (ZWSP: %d, ZWJ: %d, ZWNJ: %d markers)\n" +
            "Estimated hidden data: ~%d bytes",
            totalZwChars, zwSpaceCount, zwJoinerCount, zwNonJoinerCount, estimatedBytes
        );
    }
}
