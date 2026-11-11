package com.rdh.backend.strategy.watermark.template;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.rdh.backend.dto.request.WatermarkTemplateRequest;
import com.rdh.backend.dto.response.WatermarkTemplateResponse;
import com.rdh.backend.exception.RdhException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Strategy for embedding QR code watermarks.
 * 
 * <p>This strategy generates a QR code from text and embeds it into the cover image.
 * 
 * <p>Features:
 * <ul>
 *   <li>QR code generation from any text</li>
 *   <li>Error correction level control (L, M, Q, H)</li>
 *   <li>Size control</li>
 *   <li>Position control</li>
 *   <li>Transparency control</li>
 * </ul>
 * 
 * <p>Error Correction Levels:
 * <ul>
 *   <li>L (Low) - 7% recovery</li>
 *   <li>M (Medium) - 15% recovery (default)</li>
 *   <li>Q (Quartile) - 25% recovery</li>
 *   <li>H (High) - 30% recovery</li>
 * </ul>
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-09
 */
@Component
public class QRCodeWatermarkStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(QRCodeWatermarkStrategy.class);
    
    /**
     * Embed QR code watermark into cover image.
     * 
     * @param coverImage Cover image
     * @param request Request parameters (qrText, size, position, strength, errorCorrection)
     * @return Watermarked image
     * @throws RdhException if QR code generation fails
     */
    public BufferedImage embedQRCode(BufferedImage coverImage, WatermarkTemplateRequest request) 
            throws RdhException {
        
        logger.debug("Embedding QR code watermark (text='{}', size={}, position=({},{}), strength={})",
                request.getQrText(), request.getSize(), request.getPositionX(), 
                request.getPositionY(), request.getStrength());
        
        try {
            // Generate QR code
            BufferedImage qrCodeImage = generateQRCode(
                    request.getQrText(),
                    request.getSize(),
                    request.getQrErrorCorrection()
            );
            
            // Calculate position
            int x = (int) ((coverImage.getWidth() - qrCodeImage.getWidth()) * request.getPositionX());
            int y = (int) ((coverImage.getHeight() - qrCodeImage.getHeight()) * request.getPositionY());
            
            // Ensure QR code is within bounds
            x = Math.max(0, Math.min(x, coverImage.getWidth() - qrCodeImage.getWidth()));
            y = Math.max(0, Math.min(y, coverImage.getHeight() - qrCodeImage.getHeight()));
            
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
            
            // Draw QR code
            g2d.drawImage(qrCodeImage, x, y, null);
            
            g2d.dispose();
            
            logger.info("QR code watermark embedded successfully at position ({}, {})", x, y);
            return watermarkedImage;
            
        } catch (WriterException e) {
            logger.error("Failed to generate QR code: {}", e.getMessage());
            throw new RdhException("Failed to generate QR code: " + e.getMessage());
        }
    }
    
    /**
     * Generate QR code image from text.
     * 
     * @param text Text to encode
     * @param size Size of QR code (width and height)
     * @param errorCorrectionLevel Error correction level (L, M, Q, H)
     * @return QR code image
     * @throws WriterException if QR code generation fails
     */
    private BufferedImage generateQRCode(String text, int size, String errorCorrectionLevel) 
            throws WriterException {
        
        logger.debug("Generating QR code (size={}, errorCorrection={})", size, errorCorrectionLevel);
        
        // Map error correction level
        ErrorCorrectionLevel ecLevel = switch (errorCorrectionLevel.toUpperCase()) {
            case "L" -> ErrorCorrectionLevel.L;
            case "M" -> ErrorCorrectionLevel.M;
            case "Q" -> ErrorCorrectionLevel.Q;
            case "H" -> ErrorCorrectionLevel.H;
            default -> ErrorCorrectionLevel.M; // Default to Medium
        };
        
        // Set encoding hints
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ecLevel);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1); // Quiet zone
        
        // Generate QR code matrix
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, size, size, hints);
        
        // Convert BitMatrix to BufferedImage
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage qrImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                qrImage.setRGB(x, y, bitMatrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }
        
        logger.debug("QR code generated successfully ({}x{})", width, height);
        return qrImage;
    }
    
    /**
     * Create response for QR code watermark embedding.
     */
    public WatermarkTemplateResponse createResponse(BufferedImage watermarkedImage, WatermarkTemplateRequest request) {
        WatermarkTemplateResponse response = WatermarkTemplateResponse.success(
                "QR code watermark nhúng thành công",
                request.getTemplateType()
        );
        
        response.setQrText(request.getQrText());
        response.addMetadata("qrSize", request.getSize() + "x" + request.getSize());
        response.addMetadata("qrTextLength", request.getQrText().length());
        response.addMetadata("errorCorrection", request.getQrErrorCorrection());
        response.addMetadata("position", String.format("(%.2f, %.2f)", request.getPositionX(), request.getPositionY()));
        response.addMetadata("strength", request.getStrength());
        response.addMetadata("imageWidth", watermarkedImage.getWidth());
        response.addMetadata("imageHeight", watermarkedImage.getHeight());
        
        return response;
    }
}
