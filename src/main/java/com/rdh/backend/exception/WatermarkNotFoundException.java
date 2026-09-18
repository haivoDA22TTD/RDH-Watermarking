package com.rdh.backend.exception;

/**
 * Exception thrown when watermark cannot be found/extracted from image
 * Occurs during extraction when no watermark is detected or delimiter not found
 */
public class WatermarkNotFoundException extends RdhException {
    
    private final String reason;
    
    public WatermarkNotFoundException(String message) {
        super(message);
        this.reason = null;
    }
    
    public WatermarkNotFoundException(String message, String reason) {
        super(message);
        this.reason = reason;
    }
    
    public String getReason() {
        return reason;
    }
    
    /**
     * Create exception when delimiter not found
     */
    public static WatermarkNotFoundException delimiterNotFound(String delimiter) {
        return new WatermarkNotFoundException(
            String.format("Watermark delimiter '%s' not found in extracted data. " +
                "Image may not contain a watermark or watermark is corrupted.", delimiter),
            "DELIMITER_NOT_FOUND"
        );
    }
    
    /**
     * Create exception when no watermark detected
     */
    public static WatermarkNotFoundException noWatermarkDetected() {
        return new WatermarkNotFoundException(
            "No watermark detected in image. Possible reasons: " +
            "1) Image never had a watermark embedded, " +
            "2) Watermark was removed/corrupted by image processing, " +
            "3) Wrong extraction algorithm used.",
            "NO_WATERMARK"
        );
    }
    
    /**
     * Create exception when extracted data is invalid
     */
    public static WatermarkNotFoundException invalidExtractedData(String details) {
        return new WatermarkNotFoundException(
            String.format("Extracted data is invalid or corrupted: %s. " +
                "Watermark may have been damaged by image modifications.", details),
            "INVALID_DATA"
        );
    }
    
    /**
     * Create exception when watermark corrupted by attack
     */
    public static WatermarkNotFoundException corruptedByAttack(String attackType) {
        return new WatermarkNotFoundException(
            String.format("Watermark corrupted or destroyed by %s attack. " +
                "Try using more robust watermarking parameters or different frequency bands.", 
                attackType),
            "CORRUPTED_BY_ATTACK"
        );
    }
}
