package com.rdh.backend.service;

import com.rdh.backend.dto.request.TextStegoRequest;
import com.rdh.backend.dto.response.TextStegoResponse;

/**
 * Service interface for Text Steganography operations.
 * 
 * Provides high-level API for hiding secret data in text files using Unicode zero-width characters.
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-08
 */
public interface TextStegoService {
    
    /**
     * Embed secret data into text.
     * 
     * @param request Embed request with cover text and secret data
     * @return Response with stego text and metadata
     */
    TextStegoResponse embed(TextStegoRequest request);
    
    /**
     * Extract secret data from stego text.
     * 
     * @param stegoText Text containing hidden data
     * @return Response with extracted secret data
     */
    TextStegoResponse extract(String stegoText);
    
    /**
     * Check if text contains hidden data.
     * 
     * @param text Text to analyze
     * @return Response with detection result
     */
    TextStegoResponse detect(String text);
    
    /**
     * Calculate capacity for given text.
     * 
     * @param coverText Cover text
     * @return Response with capacity information
     */
    TextStegoResponse calculateCapacity(String coverText);
    
    /**
     * Remove hidden data from text (clean).
     * 
     * @param stegoText Text with hidden data
     * @return Response with cleaned text
     */
    TextStegoResponse clean(String stegoText);
    
    /**
     * Get statistics about hidden data.
     * 
     * @param stegoText Text to analyze
     * @return Response with statistics
     */
    TextStegoResponse analyze(String stegoText);
}
