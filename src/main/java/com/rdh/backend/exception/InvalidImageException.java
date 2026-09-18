package com.rdh.backend.exception;

/**
 * Exception thrown when image file validation fails
 * Covers: invalid format, size, dimensions, corrupted file, etc.
 */
public class InvalidImageException extends RdhException {
    
    public InvalidImageException(String message) {
        super(message);
    }
    
    public InvalidImageException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
    
    /**
     * Create exception with formatted message
     */
    public static InvalidImageException invalidFormat(String filename, String expectedFormats) {
        return new InvalidImageException(
            String.format("Invalid image format for file '%s'. Expected: %s", filename, expectedFormats)
        );
    }
    
    /**
     * Create exception for size violation
     */
    public static InvalidImageException invalidSize(String filename, long actualSize, long maxSize) {
        return new InvalidImageException(
            String.format("File '%s' size (%d bytes) exceeds maximum allowed size (%d bytes)", 
                filename, actualSize, maxSize)
        );
    }
    
    /**
     * Create exception for dimension violation
     */
    public static InvalidImageException invalidDimensions(int width, int height, int minDim, int maxDim) {
        return new InvalidImageException(
            String.format("Image dimensions (%dx%d) out of allowed range (%dx%d to %dx%d)", 
                width, height, minDim, minDim, maxDim, maxDim)
        );
    }
}
