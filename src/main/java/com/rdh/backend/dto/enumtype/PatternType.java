package com.rdh.backend.dto.enumtype;

/**
 * Enum representing different pattern types for pattern watermarking.
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-09
 */
public enum PatternType {
    
    /**
     * Grid pattern - horizontal and vertical lines forming a grid.
     */
    GRID("Grid Pattern", "Lưới ô vuông (horizontal + vertical lines)"),
    
    /**
     * Diagonal pattern - diagonal lines from top-left to bottom-right.
     */
    DIAGONAL("Diagonal Pattern", "Đường chéo từ trái sang phải"),
    
    /**
     * Anti-diagonal pattern - diagonal lines from top-right to bottom-left.
     */
    ANTI_DIAGONAL("Anti-Diagonal Pattern", "Đường chéo từ phải sang trái"),
    
    /**
     * Checkerboard pattern - alternating squares like a chess board.
     */
    CHECKERBOARD("Checkerboard Pattern", "Cờ vua (ô đen trắng xen kẽ)"),
    
    /**
     * Dots pattern - regular dots/circles in a grid.
     */
    DOTS("Dots Pattern", "Chấm tròn đều đặn"),
    
    /**
     * Cross pattern - cross/plus signs in a grid.
     */
    CROSS("Cross Pattern", "Hình chữ thập (+)");
    
    private final String name;
    private final String description;
    
    PatternType(String name, String description) {
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
