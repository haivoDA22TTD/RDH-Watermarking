package com.rdh.backend.service;

import com.rdh.backend.dto.enumtype.WatermarkAttackType;
import com.rdh.backend.dto.enumtype.WatermarkType;
import com.rdh.backend.dto.response.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service interface cho watermarking operations
 */
public interface WatermarkService {
    
    /**
     * Embed watermark vào ảnh
     * 
     * @param imageFile Ảnh gốc
     * @param watermarkText Dữ liệu watermark
     * @param algorithm Thuật toán watermarking
     * @param strength Độ mạnh (0-100)
     * @return Ảnh watermarked (PNG bytes) và metadata
     */
    WatermarkEmbedResult embedWatermark(
        MultipartFile imageFile, 
        String watermarkText, 
        WatermarkType algorithm, 
        Integer strength
    ) throws IOException;
    
    /**
     * Extract watermark từ ảnh
     * 
     * @param imageFile Ảnh watermarked
     * @param algorithm Thuật toán (phải giống lúc embed)
     * @param strength Độ mạnh (phải giống lúc embed)
     * @return Watermark đã extract và metadata
     */
    WatermarkExtractResponse extractWatermark(
        MultipartFile imageFile,
        WatermarkType algorithm,
        Integer strength
    ) throws IOException;
    
    /**
     * Test độ bền watermark với các attacks
     * 
     * @param originalImageFile Ảnh gốc (chưa watermark)
     * @param watermarkedImageFile Ảnh watermarked
     * @param attacks Danh sách attacks cần test
     * @param algorithm Thuật toán
     * @param strength Độ mạnh
     * @return Kết quả robustness test
     */
    RobustnessTestResponse testRobustness(
        MultipartFile originalImageFile,
        MultipartFile watermarkedImageFile,
        List<WatermarkAttackType> attacks,
        WatermarkType algorithm,
        Integer strength
    ) throws IOException;
    
    /**
     * Get danh sách algorithms có sẵn
     * 
     * @return List algorithm info
     */
    List<WatermarkAlgorithmInfo> getAvailableAlgorithms();
    
    /**
     * Inner class để return cả bytes và metadata
     */
    class WatermarkEmbedResult {
        private final byte[] imageBytes;
        private final WatermarkEmbedResponse metadata;
        
        public WatermarkEmbedResult(byte[] imageBytes, WatermarkEmbedResponse metadata) {
            this.imageBytes = imageBytes;
            this.metadata = metadata;
        }
        
        public byte[] getImageBytes() {
            return imageBytes;
        }
        
        public WatermarkEmbedResponse getMetadata() {
            return metadata;
        }
    }
}
