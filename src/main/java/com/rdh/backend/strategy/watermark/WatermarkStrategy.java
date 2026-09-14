package com.rdh.backend.strategy.watermark;

import java.awt.image.BufferedImage;

/**
 * Interface cho các thuật toán watermarking
 * Tương tự RdhStrategy pattern
 */
public interface WatermarkStrategy {
    
    /**
     * Embed watermark vào ảnh
     * 
     * @param originalImage Ảnh gốc
     * @param watermarkData Dữ liệu watermark (text)
     * @param strength Độ mạnh watermark (0-100)
     * @return Ảnh đã embed watermark
     */
    BufferedImage embed(BufferedImage originalImage, String watermarkData, int strength);
    
    /**
     * Extract watermark từ ảnh
     * 
     * @param watermarkedImage Ảnh có watermark
     * @param strength Độ mạnh watermark (phải giống lúc embed)
     * @return Dữ liệu watermark đã extract
     */
    String extract(BufferedImage watermarkedImage, int strength);
    
    /**
     * Get tên thuật toán
     * 
     * @return Tên thuật toán
     */
    String getAlgorithmName();
}
