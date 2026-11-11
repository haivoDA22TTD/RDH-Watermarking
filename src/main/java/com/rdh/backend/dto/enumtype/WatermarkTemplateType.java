package com.rdh.backend.dto.enumtype;

/**
 * Enum representing different types of watermark templates.
 * 
 * <p>Watermark templates allow embedding different types of watermarks:
 * <ul>
 *   <li>LOGO - Image-based watermark (upload logo file)</li>
 *   <li>QR_CODE - QR code watermark (generate from text)</li>
 *   <li>PATTERN - Custom pattern watermark (grid, diagonal, checkerboard)</li>
 * </ul>
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-09
 */
public enum WatermarkTemplateType {
    
    /**
     * Logo watermark - embed a logo image as watermark.
     * User uploads a logo file (PNG, JPG, etc.) and it's embedded into the cover image.
     */
    LOGO("Logo Watermark", "Nhúng logo/hình ảnh vào ảnh gốc"),
    
    /**
     * QR code watermark - generate QR code from text and embed.
     * User provides text, system generates QR code, then embeds it.
     */
    QR_CODE("QR Code Watermark", "Tạo mã QR từ text và nhúng vào ảnh"),
    
    /**
     * Pattern watermark - custom pattern-based watermark.
     * User selects pattern type (grid, diagonal, checkerboard) and system generates it.
     */
    PATTERN("Pattern Watermark", "Nhúng pattern tùy chỉnh (lưới, đường chéo, cờ vua)");
    
    private final String name;
    private final String description;
    
    WatermarkTemplateType(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return name + " - " + description;
    }
}
