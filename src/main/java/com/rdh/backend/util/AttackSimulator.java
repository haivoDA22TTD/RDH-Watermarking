package com.rdh.backend.util;

import com.rdh.backend.dto.enumtype.WatermarkAttackType;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;

/**
 * Utility class để simulate các attacks lên ảnh watermarked
 * Dùng để test độ bền (robustness) của watermark
 */
public class AttackSimulator {
    
    /**
     * Apply attack lên ảnh theo loại attack type
     * 
     * @param image Ảnh gốc
     * @param attackType Loại attack
     * @return Ảnh sau khi bị attack
     */
    public static BufferedImage applyAttack(BufferedImage image, WatermarkAttackType attackType) throws IOException {
        switch (attackType) {
            case JPEG_90:
                return jpegCompression(image, 0.9f);
            case JPEG_70:
                return jpegCompression(image, 0.7f);
            case JPEG_50:
                return jpegCompression(image, 0.5f);
                
            case RESIZE_75:
                return resize(image, 0.75);
            case RESIZE_50:
                return resize(image, 0.5);
            case RESIZE_25:
                return resize(image, 0.25);
                
            case CROP_5:
                return crop(image, 0.05);
            case CROP_10:
                return crop(image, 0.10);
            case CROP_20:
                return crop(image, 0.20);
                
            case GAUSSIAN_NOISE:
                return addGaussianNoise(image, 5.0);
            case SALT_PEPPER_NOISE:
                return addSaltPepperNoise(image, 0.01);
                
            case GAUSSIAN_BLUR:
                return gaussianBlur(image, 3);
            case MEDIAN_FILTER:
                return medianFilter(image, 3);
            case SHARPEN:
                return sharpen(image);
                
            case ROTATION_90:
                return rotate(image, 90);
            case ROTATION_180:
                return rotate(image, 180);
                
            default:
                return deepCopy(image);
        }
    }
    
    /**
     * JPEG compression với quality parameter
     * 
     * @param image Ảnh gốc
     * @param quality Quality (0.0 - 1.0)
     * @return Ảnh sau JPEG compression
     */
    public static BufferedImage jpegCompression(BufferedImage image, float quality) throws IOException {
        // Convert to RGB if needed
        BufferedImage rgbImage = new BufferedImage(
            image.getWidth(), 
            image.getHeight(), 
            BufferedImage.TYPE_INT_RGB
        );
        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        
        // Compress to JPEG in memory
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }
        
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgbImage, null, null), param);
            writer.dispose();
        }
        
        // Read back from compressed data
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return ImageIO.read(bais);
    }
    
    /**
     * Resize ảnh theo scale factor
     * 
     * @param image Ảnh gốc
     * @param scale Scale factor (0.0 - 1.0)
     * @return Ảnh đã resize
     */
    public static BufferedImage resize(BufferedImage image, double scale) {
        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);
        
        BufferedImage resized = new BufferedImage(newWidth, newHeight, image.getType());
        Graphics2D g = resized.createGraphics();
        
        // High quality resizing
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        
        return resized;
    }
    
    /**
     * Crop ảnh (remove border)
     * 
     * @param image Ảnh gốc
     * @param borderPercent Phần trăm viền cần crop (0.0 - 0.5)
     * @return Ảnh đã crop
     */
    public static BufferedImage crop(BufferedImage image, double borderPercent) {
        int cropX = (int) (image.getWidth() * borderPercent);
        int cropY = (int) (image.getHeight() * borderPercent);
        
        int newWidth = image.getWidth() - 2 * cropX;
        int newHeight = image.getHeight() - 2 * cropY;
        
        if (newWidth <= 0 || newHeight <= 0) {
            return deepCopy(image); // Không crop nếu quá nhỏ
        }
        
        return image.getSubimage(cropX, cropY, newWidth, newHeight);
    }
    
    /**
     * Add Gaussian noise
     * 
     * @param image Ảnh gốc
     * @param sigma Độ lệch chuẩn của noise
     * @return Ảnh với noise
     */
    public static BufferedImage addGaussianNoise(BufferedImage image, double sigma) {
        BufferedImage noisy = deepCopy(image);
        Random random = new Random();
        
        for (int y = 0; y < noisy.getHeight(); y++) {
            for (int x = 0; x < noisy.getWidth(); x++) {
                int rgb = noisy.getRGB(x, y);
                
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                
                // Add Gaussian noise
                r = clamp((int) (r + random.nextGaussian() * sigma));
                g = clamp((int) (g + random.nextGaussian() * sigma));
                b = clamp((int) (b + random.nextGaussian() * sigma));
                
                int newRgb = (r << 16) | (g << 8) | b;
                noisy.setRGB(x, y, newRgb);
            }
        }
        
        return noisy;
    }
    
    /**
     * Add Salt & Pepper noise
     * 
     * @param image Ảnh gốc
     * @param density Mật độ noise (0.0 - 1.0)
     * @return Ảnh với noise
     */
    public static BufferedImage addSaltPepperNoise(BufferedImage image, double density) {
        BufferedImage noisy = deepCopy(image);
        Random random = new Random();
        
        int numPixels = (int) (image.getWidth() * image.getHeight() * density);
        
        for (int i = 0; i < numPixels; i++) {
            int x = random.nextInt(noisy.getWidth());
            int y = random.nextInt(noisy.getHeight());
            
            // Random salt (white) or pepper (black)
            int color = random.nextBoolean() ? 0xFFFFFF : 0x000000;
            noisy.setRGB(x, y, color);
        }
        
        return noisy;
    }
    
    /**
     * Apply Gaussian blur
     * 
     * @param image Ảnh gốc
     * @param kernelSize Kích thước kernel (phải là số lẻ)
     * @return Ảnh đã blur
     */
    public static BufferedImage gaussianBlur(BufferedImage image, int kernelSize) {
        if (kernelSize % 2 == 0) kernelSize++; // Ensure odd
        
        double sigma = kernelSize / 3.0;
        double[][] kernel = createGaussianKernel(kernelSize, sigma);
        
        return applyConvolution(image, kernel);
    }
    
    /**
     * Apply median filter
     * 
     * @param image Ảnh gốc
     * @param kernelSize Kích thước kernel
     * @return Ảnh đã filter
     */
    public static BufferedImage medianFilter(BufferedImage image, int kernelSize) {
        BufferedImage filtered = deepCopy(image);
        int radius = kernelSize / 2;
        
        for (int y = radius; y < image.getHeight() - radius; y++) {
            for (int x = radius; x < image.getWidth() - radius; x++) {
                int[] rValues = new int[kernelSize * kernelSize];
                int[] gValues = new int[kernelSize * kernelSize];
                int[] bValues = new int[kernelSize * kernelSize];
                int idx = 0;
                
                for (int ky = -radius; ky <= radius; ky++) {
                    for (int kx = -radius; kx <= radius; kx++) {
                        int rgb = image.getRGB(x + kx, y + ky);
                        rValues[idx] = (rgb >> 16) & 0xFF;
                        gValues[idx] = (rgb >> 8) & 0xFF;
                        bValues[idx] = rgb & 0xFF;
                        idx++;
                    }
                }
                
                // Find median
                int r = median(rValues);
                int g = median(gValues);
                int b = median(bValues);
                
                int newRgb = (r << 16) | (g << 8) | b;
                filtered.setRGB(x, y, newRgb);
            }
        }
        
        return filtered;
    }
    
    /**
     * Apply sharpen filter
     * 
     * @param image Ảnh gốc
     * @return Ảnh đã sharpen
     */
    public static BufferedImage sharpen(BufferedImage image) {
        double[][] kernel = {
            { 0, -1,  0},
            {-1,  5, -1},
            { 0, -1,  0}
        };
        
        return applyConvolution(image, kernel);
    }
    
    /**
     * Rotate ảnh
     * 
     * @param image Ảnh gốc
     * @param degrees Góc xoay (90, 180, 270)
     * @return Ảnh đã xoay
     */
    public static BufferedImage rotate(BufferedImage image, int degrees) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        BufferedImage rotated;
        if (degrees == 90 || degrees == 270) {
            rotated = new BufferedImage(height, width, image.getType());
        } else {
            rotated = new BufferedImage(width, height, image.getType());
        }
        
        Graphics2D g = rotated.createGraphics();
        
        if (degrees == 90) {
            g.translate(height, 0);
            g.rotate(Math.toRadians(90));
        } else if (degrees == 180) {
            g.translate(width, height);
            g.rotate(Math.toRadians(180));
        } else if (degrees == 270) {
            g.translate(0, width);
            g.rotate(Math.toRadians(270));
        }
        
        g.drawImage(image, 0, 0, null);
        g.dispose();
        
        return rotated;
    }
    
    // ============= Helper methods =============
    
    /**
     * Create Gaussian kernel
     */
    private static double[][] createGaussianKernel(int size, double sigma) {
        double[][] kernel = new double[size][size];
        double sum = 0.0;
        int radius = size / 2;
        
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                double value = Math.exp(-(x * x + y * y) / (2 * sigma * sigma));
                kernel[y + radius][x + radius] = value;
                sum += value;
            }
        }
        
        // Normalize
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                kernel[y][x] /= sum;
            }
        }
        
        return kernel;
    }
    
    /**
     * Apply convolution với kernel
     */
    private static BufferedImage applyConvolution(BufferedImage image, double[][] kernel) {
        BufferedImage result = deepCopy(image);
        int kernelSize = kernel.length;
        int radius = kernelSize / 2;
        
        for (int y = radius; y < image.getHeight() - radius; y++) {
            for (int x = radius; x < image.getWidth() - radius; x++) {
                double r = 0, g = 0, b = 0;
                
                for (int ky = 0; ky < kernelSize; ky++) {
                    for (int kx = 0; kx < kernelSize; kx++) {
                        int px = x + kx - radius;
                        int py = y + ky - radius;
                        int rgb = image.getRGB(px, py);
                        
                        double weight = kernel[ky][kx];
                        r += ((rgb >> 16) & 0xFF) * weight;
                        g += ((rgb >> 8) & 0xFF) * weight;
                        b += (rgb & 0xFF) * weight;
                    }
                }
                
                int newRgb = (clamp((int) r) << 16) | (clamp((int) g) << 8) | clamp((int) b);
                result.setRGB(x, y, newRgb);
            }
        }
        
        return result;
    }
    
    /**
     * Find median of array
     */
    private static int median(int[] values) {
        int[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }
    
    /**
     * Clamp value to 0-255
     */
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
    
    /**
     * Deep copy BufferedImage
     */
    private static BufferedImage deepCopy(BufferedImage image) {
        BufferedImage copy = new BufferedImage(
            image.getWidth(), 
            image.getHeight(), 
            image.getType()
        );
        Graphics2D g = copy.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return copy;
    }
}
