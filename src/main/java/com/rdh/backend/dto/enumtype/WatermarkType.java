package com.rdh.backend.dto.enumtype;

/**
 * Enum định nghĩa các loại thuật toán watermarking được hỗ trợ
 */
public enum WatermarkType {
    
    /**
     * DCT-based Watermarking (Discrete Cosine Transform)
     * - Độ bền: Cao
     * - Chống JPEG compression, resize tốt
     * - PSNR: 40-50 dB
     * - Capacity: 256-512 bits
     * - Recommended cho production
     */
    DCT("DCT-based Watermarking", "Robust against JPEG compression and resizing", "HIGH", 256),
    
    /**
     * DWT-based Watermarking (Discrete Wavelet Transform)
     * - Độ bền: Rất cao
     * - Chống nhiều loại tấn công
     * - PSNR: 38-45 dB
     * - Capacity: 128-256 bits
     * - Phù hợp cho y tế, pháp lý
     */
    DWT("DWT-based Watermarking", "Very robust, suitable for medical/legal images", "VERY_HIGH", 128),
    
    /**
     * LSB Robust Watermarking (với error correction)
     * - Độ bền: Thấp-Trung bình
     * - Chống visual attacks
     * - PSNR: 50+ dB
     * - Capacity: 512-1024 bits
     * - Phù hợp cho demo
     */
    LSB_ROBUST("LSB with Error Correction", "Simple but less robust, high capacity", "LOW", 512);
    
    private final String displayName;
    private final String description;
    private final String robustness;
    private final int typicalCapacity; // bits
    
    WatermarkType(String displayName, String description, String robustness, int typicalCapacity) {
        this.displayName = displayName;
        this.description = description;
        this.robustness = robustness;
        this.typicalCapacity = typicalCapacity;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getRobustness() {
        return robustness;
    }
    
    public int getTypicalCapacity() {
        return typicalCapacity;
    }
}
