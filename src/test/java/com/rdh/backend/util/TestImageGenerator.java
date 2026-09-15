package com.rdh.backend.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Utility class để generate test images với các patterns khác nhau
 */
public class TestImageGenerator {
    
    private static final Random random = new Random(12345); // Fixed seed for reproducibility
    
    /**
     * Tạo ảnh solid color đơn giản
     */
    public static BufferedImage createSolidColorImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }
    
    /**
     * Tạo ảnh với random noise
     */
    public static BufferedImage createRandomNoiseImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = random.nextInt(256);
                int g = random.nextInt(256);
                int b = random.nextInt(256);
                int rgb = (r << 16) | (g << 8) | b;
                image.setRGB(x, y, rgb);
            }
        }
        
        return image;
    }
    
    /**
     * Tạo ảnh gradient (từ đen sang trắng)
     */
    public static BufferedImage createGradientImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = (int) (255.0 * x / width);
                int rgb = (value << 16) | (value << 8) | value;
                image.setRGB(x, y, rgb);
            }
        }
        
        return image;
    }
    
    /**
     * Tạo ảnh checkerboard pattern
     */
    public static BufferedImage createCheckerboardImage(int width, int height, int blockSize) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        for (int y = 0; y < height; y += blockSize) {
            for (int x = 0; x < width; x += blockSize) {
                boolean isBlack = ((x / blockSize) + (y / blockSize)) % 2 == 0;
                g.setColor(isBlack ? Color.BLACK : Color.WHITE);
                g.fillRect(x, y, blockSize, blockSize);
            }
        }
        
        g.dispose();
        return image;
    }
    
    /**
     * Tạo ảnh với text
     */
    public static BufferedImage createImageWithText(int width, int height, String text) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // White background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        
        // Black text
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 48));
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int x = (width - textWidth) / 2;
        int y = (height - textHeight) / 2 + fm.getAscent();
        g.drawString(text, x, y);
        
        g.dispose();
        return image;
    }
    
    /**
     * Tạo ảnh test standard 256x256
     */
    public static BufferedImage createStandardTestImage() {
        return createGradientImage(256, 256);
    }
    
    /**
     * Tạo ảnh test nhỏ 64x64 cho unit tests nhanh
     */
    public static BufferedImage createSmallTestImage() {
        return createGradientImage(64, 64);
    }
    
    /**
     * Tạo ảnh test lớn 512x512
     */
    public static BufferedImage createLargeTestImage() {
        return createRandomNoiseImage(512, 512);
    }
    
    /**
     * Tạo ảnh với histogram đặc biệt (nhiều pixels có cùng giá trị)
     * Phù hợp để test Histogram Shifting
     */
    public static BufferedImage createHistogramShiftingTestImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // Tạo nhiều pixels có giá trị 128 (peak)
        int peakValue = 128;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value;
                if (random.nextDouble() < 0.6) {
                    // 60% pixels có giá trị gần peak
                    value = peakValue + random.nextInt(21) - 10; // ±10
                } else {
                    // 40% còn lại random
                    value = random.nextInt(256);
                }
                
                value = Math.max(0, Math.min(255, value));
                int rgb = (value << 16) | (value << 8) | value;
                image.setRGB(x, y, rgb);
            }
        }
        
        return image;
    }
    
    /**
     * Clone một BufferedImage
     */
    public static BufferedImage cloneImage(BufferedImage source) {
        BufferedImage clone = new BufferedImage(
            source.getWidth(),
            source.getHeight(),
            source.getType()
        );
        Graphics2D g = clone.createGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return clone;
    }
    
    /**
     * So sánh 2 ảnh có giống nhau không (pixel-by-pixel)
     */
    public static boolean areImagesEqual(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }
        
        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Tính MSE (Mean Squared Error) giữa 2 ảnh
     */
    public static double calculateMSE(BufferedImage img1, BufferedImage img2) {
        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            throw new IllegalArgumentException("Images must have same dimensions");
        }
        
        double mse = 0.0;
        int pixels = img1.getWidth() * img1.getHeight();
        
        for (int y = 0; y < img1.getHeight(); y++) {
            for (int x = 0; x < img1.getWidth(); x++) {
                int rgb1 = img1.getRGB(x, y);
                int rgb2 = img2.getRGB(x, y);
                
                int r1 = (rgb1 >> 16) & 0xFF;
                int g1 = (rgb1 >> 8) & 0xFF;
                int b1 = rgb1 & 0xFF;
                
                int r2 = (rgb2 >> 16) & 0xFF;
                int g2 = (rgb2 >> 8) & 0xFF;
                int b2 = rgb2 & 0xFF;
                
                mse += Math.pow(r1 - r2, 2);
                mse += Math.pow(g1 - g2, 2);
                mse += Math.pow(b1 - b2, 2);
            }
        }
        
        return mse / (pixels * 3);
    }
}
