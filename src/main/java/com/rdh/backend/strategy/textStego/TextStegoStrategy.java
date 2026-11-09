package com.rdh.backend.strategy.textStego;

/**
 * Strategy interface for text steganography algorithms.
 * 
 * Text steganography hides secret data within text files using various techniques:
 * - Unicode zero-width characters
 * - Whitespace encoding
 * - Format manipulation
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-08
 */
public interface TextStegoStrategy {
    
    /**
     * Embed secret data into cover text.
     * 
     * @param coverText Original text (plain text, markdown, JSON, code, etc.)
     * @param secretData Secret message to hide
     * @return Stego text with embedded secret data
     * @throws IllegalArgumentException if inputs are invalid
     */
    String embed(String coverText, String secretData);
    
    /**
     * Extract secret data from stego text.
     * 
     * @param stegoText Text containing hidden data
     * @return Extracted secret data
     * @throws IllegalArgumentException if stegoText is invalid
     * @throws com.rdh.backend.exception.WatermarkNotFoundException if no hidden data found
     */
    String extract(String stegoText);
    
    /**
     * Check if text contains hidden data.
     * 
     * @param text Text to analyze
     * @return true if hidden data detected, false otherwise
     */
    boolean hasHiddenData(String text);
    
    /**
     * Calculate capacity - how many bytes can be hidden in given text.
     * 
     * @param coverText Cover text
     * @return Capacity in bytes
     */
    long calculateCapacity(String coverText);
    
    /**
     * Get algorithm name.
     * 
     * @return Algorithm name (e.g., "Unicode Zero-Width", "Whitespace")
     */
    String getAlgorithmName();
}
