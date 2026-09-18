package com.rdh.backend.exception;

/**
 * Exception thrown when requested algorithm is not supported or not available
 * Covers: unknown algorithm type, disabled algorithm, incompatible parameters
 */
public class UnsupportedAlgorithmException extends RdhException {
    
    private final String algorithmName;
    private final String availableAlgorithms;
    
    public UnsupportedAlgorithmException(String message) {
        super(message);
        this.algorithmName = null;
        this.availableAlgorithms = null;
    }
    
    public UnsupportedAlgorithmException(String algorithmName, String availableAlgorithms) {
        super(formatMessage(algorithmName, availableAlgorithms));
        this.algorithmName = algorithmName;
        this.availableAlgorithms = availableAlgorithms;
    }
    
    private static String formatMessage(String algorithmName, String availableAlgorithms) {
        return String.format(
            "Algorithm '%s' is not supported. Available algorithms: %s",
            algorithmName, availableAlgorithms
        );
    }
    
    public String getAlgorithmName() {
        return algorithmName;
    }
    
    public String getAvailableAlgorithms() {
        return availableAlgorithms;
    }
    
    /**
     * Create exception for unknown RDH algorithm
     */
    public static UnsupportedAlgorithmException unknownRdhAlgorithm(String algorithmName) {
        return new UnsupportedAlgorithmException(
            algorithmName,
            "LSB, HISTOGRAM_SHIFTING, DIFFERENCE_EXPANSION"
        );
    }
    
    /**
     * Create exception for unknown watermark type
     */
    public static UnsupportedAlgorithmException unknownWatermarkType(String type) {
        return new UnsupportedAlgorithmException(
            type,
            "DCT, DWT, SVD"
        );
    }
    
    /**
     * Create exception for unsupported attack type
     */
    public static UnsupportedAlgorithmException unsupportedAttackType(String attackType) {
        return new UnsupportedAlgorithmException(
            String.format("Attack type '%s' is not supported or cannot be simulated. " +
                "Available attack types: JPEG_COMPRESSION, GAUSSIAN_NOISE, SALT_PEPPER_NOISE, " +
                "SCALING, ROTATION, CROPPING, MEDIAN_FILTER, GAUSSIAN_BLUR", attackType)
        );
    }
    
    /**
     * Create exception for incompatible parameters
     */
    public static UnsupportedAlgorithmException incompatibleParameters(
            String algorithmName, String parameterIssue) {
        return new UnsupportedAlgorithmException(
            String.format("Algorithm '%s' cannot be used with current parameters: %s",
                algorithmName, parameterIssue)
        );
    }
}
