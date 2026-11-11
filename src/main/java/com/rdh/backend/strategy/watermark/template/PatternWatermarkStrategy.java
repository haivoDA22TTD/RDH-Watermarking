package com.rdh.backend.strategy.watermark.template;

import com.rdh.backend.dto.enumtype.PatternType;
import com.rdh.backend.dto.request.WatermarkTemplateRequest;
import com.rdh.backend.dto.response.WatermarkTemplateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Strategy for embedding pattern-based watermarks.
 * 
 * <p>This strategy generates custom patterns and embeds them into the cover image.
 * 
 * <p>Supported patterns:
 * <ul>
 *   <li>GRID - Horizontal and vertical lines forming a grid</li>
 *   <li>DIAGONAL - Diagonal lines from top-left to bottom-right</li>
 *   <li>ANTI_DIAGONAL - Diagonal lines from top-right to bottom-left</li>
 *   <li>CHECKERBOARD - Alternating squares like a chess board</li>
 *   <li>DOTS - Regular dots in a grid</li>
 *   <li>CROSS - Cross/plus signs in a grid</li>
 * </ul>
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-09
 */
@Component
public class PatternWatermarkStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(PatternWatermarkStrategy.class);
    
    /**
     * Embed pattern watermark into cover image.
     * 
     * @param coverImage Cover image
     * @param request Request parameters (patternType, spacing, strength)
     * @return Watermarked image
     */
    public BufferedImage embedPattern(BufferedImage coverImage, WatermarkTemplateRequest request) {
        
        logger.debug("Embedding pattern watermark (type={}, spacing={}, strength={})",
                request.getPatternType(), request.getSpacing(), request.getStrength());
        
        // Create watermarked image
        BufferedImage watermarkedImage = new BufferedImage(
                coverImage.getWidth(),
                coverImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        
        Graphics2D g2d = watermarkedImage.createGraphics();
        
        // Draw original image
        g2d.drawImage(coverImage, 0, 0, null);
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Set transparency and color
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, request.getStrength().floatValue()));
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2.0f));
        
        // Draw pattern based on type
        switch (request.getPatternType()) {
            case GRID -> drawGrid(g2d, coverImage, request.getSpacing());
            case DIAGONAL -> drawDiagonal(g2d, coverImage, request.getSpacing());
            case ANTI_DIAGONAL -> drawAntiDiagonal(g2d, coverImage, request.getSpacing());
            case CHECKERBOARD -> drawCheckerboard(g2d, coverImage, request.getSpacing());
            case DOTS -> drawDots(g2d, coverImage, request.getSpacing());
            case CROSS -> drawCross(g2d, coverImage, request.getSpacing());
        }
        
        g2d.dispose();
        
        logger.info("Pattern watermark embedded successfully (type={})", request.getPatternType());
        return watermarkedImage;
    }
    
    /**
     * Draw grid pattern (horizontal + vertical lines).
     */
    private void drawGrid(Graphics2D g2d, BufferedImage image, int spacing) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Vertical lines
        for (int x = 0; x < width; x += spacing) {
            g2d.drawLine(x, 0, x, height);
        }
        
        // Horizontal lines
        for (int y = 0; y < height; y += spacing) {
            g2d.drawLine(0, y, width, y);
        }
        
        logger.debug("Drew grid pattern (spacing={})", spacing);
    }
    
    /**
     * Draw diagonal pattern (top-left to bottom-right).
     */
    private void drawDiagonal(Graphics2D g2d, BufferedImage image, int spacing) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Diagonal lines starting from top edge
        for (int x = 0; x < width; x += spacing) {
            g2d.drawLine(x, 0, x + height, height);
        }
        
        // Diagonal lines starting from left edge
        for (int y = spacing; y < height; y += spacing) {
            g2d.drawLine(0, y, width, y + width);
        }
        
        logger.debug("Drew diagonal pattern (spacing={})", spacing);
    }
    
    /**
     * Draw anti-diagonal pattern (top-right to bottom-left).
     */
    private void drawAntiDiagonal(Graphics2D g2d, BufferedImage image, int spacing) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Diagonal lines starting from top edge
        for (int x = 0; x < width; x += spacing) {
            g2d.drawLine(x, 0, x - height, height);
        }
        
        // Diagonal lines starting from right edge
        for (int y = spacing; y < height; y += spacing) {
            g2d.drawLine(width, y, 0, y + width);
        }
        
        logger.debug("Drew anti-diagonal pattern (spacing={})", spacing);
    }
    
    /**
     * Draw checkerboard pattern (alternating squares).
     */
    private void drawCheckerboard(Graphics2D g2d, BufferedImage image, int spacing) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        boolean startWhite = true;
        for (int y = 0; y < height; y += spacing) {
            boolean drawWhite = startWhite;
            for (int x = 0; x < width; x += spacing) {
                if (drawWhite) {
                    g2d.fillRect(x, y, spacing, spacing);
                }
                drawWhite = !drawWhite;
            }
            startWhite = !startWhite;
        }
        
        logger.debug("Drew checkerboard pattern (spacing={})", spacing);
    }
    
    /**
     * Draw dots pattern (circles in a grid).
     */
    private void drawDots(Graphics2D g2d, BufferedImage image, int spacing) {
        int width = image.getWidth();
        int height = image.getHeight();
        int dotSize = Math.max(5, spacing / 5); // Dot size is 20% of spacing
        
        for (int y = 0; y < height; y += spacing) {
            for (int x = 0; x < width; x += spacing) {
                g2d.fillOval(x - dotSize / 2, y - dotSize / 2, dotSize, dotSize);
            }
        }
        
        logger.debug("Drew dots pattern (spacing={}, dotSize={})", spacing, dotSize);
    }
    
    /**
     * Draw cross pattern (+ signs in a grid).
     */
    private void drawCross(Graphics2D g2d, BufferedImage image, int spacing) {
        int width = image.getWidth();
        int height = image.getHeight();
        int crossSize = spacing / 3; // Cross size is 33% of spacing
        
        for (int y = 0; y < height; y += spacing) {
            for (int x = 0; x < width; x += spacing) {
                // Vertical line of cross
                g2d.drawLine(x, y - crossSize, x, y + crossSize);
                // Horizontal line of cross
                g2d.drawLine(x - crossSize, y, x + crossSize, y);
            }
        }
        
        logger.debug("Drew cross pattern (spacing={}, crossSize={})", spacing, crossSize);
    }
    
    /**
     * Create response for pattern watermark embedding.
     */
    public WatermarkTemplateResponse createResponse(BufferedImage watermarkedImage, WatermarkTemplateRequest request) {
        WatermarkTemplateResponse response = WatermarkTemplateResponse.success(
                "Pattern watermark nhúng thành công",
                request.getTemplateType()
        );
        
        response.setPatternType(request.getPatternType());
        response.addMetadata("patternType", request.getPatternType().getName());
        response.addMetadata("patternDescription", request.getPatternType().getDescription());
        response.addMetadata("spacing", request.getSpacing());
        response.addMetadata("strength", request.getStrength());
        response.addMetadata("imageWidth", watermarkedImage.getWidth());
        response.addMetadata("imageHeight", watermarkedImage.getHeight());
        
        // Calculate approximate number of pattern elements
        int elementsX = watermarkedImage.getWidth() / request.getSpacing();
        int elementsY = watermarkedImage.getHeight() / request.getSpacing();
        response.addMetadata("patternElements", elementsX * elementsY);
        
        return response;
    }
}
