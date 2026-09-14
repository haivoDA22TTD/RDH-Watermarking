package com.rdh.backend.dto.enumtype;

/**
 * Enum định nghĩa các loại tấn công (attacks) để test độ bền watermark
 */
public enum WatermarkAttackType {
    
    /**
     * JPEG Compression với chất lượng 90%
     * - Tấn công nhẹ
     * - Watermark tốt nên survive
     */
    JPEG_90("JPEG Compression 90%", "Light JPEG compression", "LIGHT"),
    
    /**
     * JPEG Compression với chất lượng 70%
     * - Tấn công trung bình
     * - Test case phổ biến nhất
     */
    JPEG_70("JPEG Compression 70%", "Medium JPEG compression", "MEDIUM"),
    
    /**
     * JPEG Compression với chất lượng 50%
     * - Tấn công nặng
     * - Watermark yếu sẽ mất
     */
    JPEG_50("JPEG Compression 50%", "Heavy JPEG compression", "HEAVY"),
    
    /**
     * Resize ảnh xuống 75% kích thước gốc
     */
    RESIZE_75("Resize to 75%", "Scale down to 75% of original size", "LIGHT"),
    
    /**
     * Resize ảnh xuống 50% kích thước gốc
     */
    RESIZE_50("Resize to 50%", "Scale down to 50% of original size", "MEDIUM"),
    
    /**
     * Resize ảnh xuống 25% kích thước gốc
     */
    RESIZE_25("Resize to 25%", "Scale down to 25% of original size", "HEAVY"),
    
    /**
     * Crop 5% viền ảnh
     */
    CROP_5("Crop 5% border", "Remove 5% border from all sides", "LIGHT"),
    
    /**
     * Crop 10% viền ảnh
     */
    CROP_10("Crop 10% border", "Remove 10% border from all sides", "MEDIUM"),
    
    /**
     * Crop 20% viền ảnh
     */
    CROP_20("Crop 20% border", "Remove 20% border from all sides", "HEAVY"),
    
    /**
     * Thêm Gaussian noise (nhiễu Gauss) - sigma = 5
     */
    GAUSSIAN_NOISE("Gaussian Noise", "Add Gaussian noise with sigma=5", "MEDIUM"),
    
    /**
     * Salt & Pepper noise - 1% pixels
     */
    SALT_PEPPER_NOISE("Salt & Pepper Noise", "Add salt & pepper noise (1% pixels)", "MEDIUM"),
    
    /**
     * Gaussian blur - kernel 3x3
     */
    GAUSSIAN_BLUR("Gaussian Blur", "Apply Gaussian blur (3x3 kernel)", "LIGHT"),
    
    /**
     * Median filter - kernel 3x3
     */
    MEDIAN_FILTER("Median Filter", "Apply median filter (3x3 kernel)", "LIGHT"),
    
    /**
     * Sharpen filter
     */
    SHARPEN("Sharpen", "Apply sharpening filter", "LIGHT"),
    
    /**
     * Rotation 90 degrees
     */
    ROTATION_90("Rotate 90°", "Rotate image 90 degrees clockwise", "MEDIUM"),
    
    /**
     * Rotation 180 degrees
     */
    ROTATION_180("Rotate 180°", "Rotate image 180 degrees", "MEDIUM");
    
    private final String displayName;
    private final String description;
    private final String severity;
    
    WatermarkAttackType(String displayName, String description, String severity) {
        this.displayName = displayName;
        this.description = description;
        this.severity = severity;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getSeverity() {
        return severity;
    }
}
