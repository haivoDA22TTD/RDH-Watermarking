package com.rdh.backend.strategy.watermark.template;

import com.rdh.backend.dto.request.WatermarkTemplateRequest;
import com.rdh.backend.dto.response.WatermarkTemplateResponse;
import com.rdh.backend.exception.InvalidImageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Strategy for embedding logo/image watermarks.
 * 
 * <p>This strategy takes a logo image and embeds it into the cover image
 * at the specified position with the specified transparency/strength.
 * 
 * <p>Features:
 * <ul>
 *   <li>Position control (top-left, center, bottom-right, custom)</li>
 *   <li>Size control (resize logo to fit)</li>
 *   <li>Transparency/alpha control (0.0-1.0)</li>
 *   <li>Automatic aspect ratio preservation</li>
 * </ul>
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-09
 */
@Component
public class LogoWatermarkStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(LogoWatermarkStrategy.class);
    
    /**
     * Embed logo watermark into cover image.
     * 
     * @param coverImage Cover image
     * @param logoBytes Logo image bytes
     * @param request Request parameters (position, size, strength)
     * @return Watermarked image
     * @throws InvalidImageException if logo image is invalid
     */
    public BufferedImage embedLogo(BufferedImage coverImage, byte[] logoBytes, WatermarkTemplateRequest request) 
            throws InvalidImageException {
        
        logger.debug("Embedding logo watermark (size={}, position=({},{}), strength={})",
                request.getSize(), request.getPositionX(), request.getPositionY(), request.getStrength());
        
        try {
            // Read logo image
            BufferedImage logoImage = ImageIO.read(new ByteArrayInputStream(logoBytes));
            if (logoImage == null) {
                throw new InvalidImageException("Cannot read logo image. Unsupported format or corrupted file.");
            }
            
            // Resize logo to requested size (preserve aspect ratio)
            BufferedImage resizedLogo = resizeLogo(logoImage, request.getSize());
            
            // Calculate position
            int x = (int) ((coverImage.getWidth() - resizedLogo.getWidth()) * request.getPositionX());
            int y = (int) ((coverImage.getHeight() - resizedLogo.getHeight()) * request.getPositionY());
            
            // Ensure logo is within bounds
            x = Math.max(0, Math.min(x, coverImage.getWidth() - resizedLogo.getWidth()));
            y = Math.max(0, Math.min(y, coverImage.getHeight() - resizedLogo.getHeight()));
            
            // Create watermarked image
            BufferedImage watermarkedImage = new BufferedImage(
                    coverImage.getWidth(),
                    coverImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB
            );
            
            Graphics2D g2d = watermarkedImage.createGraphics();
            
            // Draw original image
            g2d.drawImage(coverImage, 0, 0, null);
            
            // Set transparency
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, request.getStrength().floatValue()));
            
            // Draw logo
            g2d.drawImage(resizedLogo, x, y, null);
            
            g2d.dispose();
            
            logger.info("Logo watermark embedded successfully at position ({}, {})", x, y);
            return watermarkedImage;
            
        } catch (IOException e) {
            logger.error("Failed to embed logo watermark: {}", e.getMessage());
            throw new InvalidImageException("Failed to embed logo watermark: " + e.getMessage());
        }
    }
    
    /**
     * Resize logo image to target size while preserving aspect ratio.
     * 
     * @param logo Original logo image
     * @param targetSize Target size (width or height, whichever is larger)
     * @return Resized logo
     */
    private BufferedImage resizeLogo(BufferedImage logo, int targetSize) {
        int originalWidth = logo.getWidth();
        int originalHeight = logo.getHeight();
        
        double aspectRatio = (double) originalWidth / originalHeight;
        
        int newWidth, newHeight;
        if (originalWidth > originalHeight) {
            newWidth = targetSize;
            newHeight = (int) (targetSize / aspectRatio);
        } else {
            newHeight = targetSize;
            newWidth = (int) (targetSize * aspectRatio);
        }
        
        logger.debug("Resizing logo from {}x{} to {}x{}", 
                originalWidth, originalHeight, newWidth, newHeight);
        
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        
        // Use high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2d.drawImage(logo, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        
        return resized;
    }
    
    /**
     * Create response for logo watermark embedding.
     */
    public WatermarkTemplateResponse createResponse(BufferedImage watermarkedImage, WatermarkTemplateRequest request) {
        WatermarkTemplateResponse response = WatermarkTemplateResponse.success(
                "Logo watermark nhúng thành công",
                request.getTemplateType()
        );
        
        response.addMetadata("logoSize", request.getSize() + "x" + request.getSize());
        response.addMetadata("position", String.format("(%.2f, %.2f)", request.getPositionX(), request.getPositionY()));
        response.addMetadata("strength", request.getStrength());
        response.addMetadata("imageWidth", watermarkedImage.getWidth());
        response.addMetadata("imageHeight", watermarkedImage.getHeight());
        
        return response;
    }
}
