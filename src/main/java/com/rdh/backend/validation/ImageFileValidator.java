package com.rdh.backend.validation;

import com.rdh.backend.exception.InvalidImageException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Validator cho image files
 * Validate file size, format, dimensions, và content
 */
@Component
public class ImageFileValidator {
    
    // Constants
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB cho single upload
    private static final long MAX_BATCH_FILE_SIZE = 200 * 1024 * 1024; // 200MB cho batch
    private static final int MIN_DIMENSION = 64; // Minimum 64x64
    private static final int MAX_DIMENSION = 4096; // Maximum 4096x4096
    
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
        "image/png",
        "image/jpeg",
        "image/jpg",
        "image/bmp"
    );
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "png", "jpg", "jpeg", "bmp"
    );
    
    /**
     * Validate image file cho single upload
     * 
     * @param file MultipartFile to validate
     * @throws InvalidImageException if validation fails
     */
    public void validateImageFile(MultipartFile file) {
        validateImageFile(file, false);
    }
    
    /**
     * Validate image file with batch flag
     * 
     * @param file MultipartFile to validate
     * @param isBatch true if this is batch upload
     * @throws InvalidImageException if validation fails
     */
    public void validateImageFile(MultipartFile file, boolean isBatch) {
        // 1. Check file not null
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("Image file is required and cannot be empty");
        }
        
        // 2. Check file size
        long maxSize = isBatch ? MAX_BATCH_FILE_SIZE : MAX_FILE_SIZE;
        if (file.getSize() > maxSize) {
            throw new InvalidImageException(
                String.format("File size exceeds maximum allowed size of %d MB", maxSize / (1024 * 1024))
            );
        }
        
        if (file.getSize() == 0) {
            throw new InvalidImageException("File is empty (0 bytes)");
        }
        
        // 3. Check content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidImageException(
                String.format("Invalid content type: %s. Allowed types: %s", 
                    contentType, ALLOWED_CONTENT_TYPES)
            );
        }
        
        // 4. Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasValidExtension(originalFilename)) {
            throw new InvalidImageException(
                String.format("Invalid file extension. Allowed extensions: %s", ALLOWED_EXTENSIONS)
            );
        }
        
        // 5. Validate image content (try to read as image)
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new InvalidImageException("Cannot read file as image. File may be corrupted or not a valid image format");
            }
            
            // 6. Check dimensions
            int width = image.getWidth();
            int height = image.getHeight();
            
            if (width < MIN_DIMENSION || height < MIN_DIMENSION) {
                throw new InvalidImageException(
                    String.format("Image dimensions too small. Minimum: %dx%d, Actual: %dx%d",
                        MIN_DIMENSION, MIN_DIMENSION, width, height)
                );
            }
            
            if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
                throw new InvalidImageException(
                    String.format("Image dimensions too large. Maximum: %dx%d, Actual: %dx%d",
                        MAX_DIMENSION, MAX_DIMENSION, width, height)
                );
            }
            
            // 7. Check image must be divisible by 8 (for DCT blocks)
            if (width % 8 != 0 || height % 8 != 0) {
                // Warning but not error - will handle with padding
                System.out.println(
                    String.format("Warning: Image dimensions (%dx%d) not divisible by 8. Will use partial blocks.",
                        width, height)
                );
            }
            
        } catch (IOException e) {
            throw new InvalidImageException("Failed to read image file: " + e.getMessage());
        }
    }
    
    /**
     * Check if filename has valid extension
     */
    private boolean hasValidExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return false;
        }
        
        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
    
    /**
     * Validate multiple image files (for batch operations)
     * 
     * @param files List of files to validate
     * @throws InvalidImageException if any file fails validation
     */
    public void validateImageFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new InvalidImageException("At least one image file is required");
        }
        
        // Check total size
        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalSize > MAX_BATCH_FILE_SIZE) {
            throw new InvalidImageException(
                String.format("Total batch size exceeds maximum allowed size of %d MB", 
                    MAX_BATCH_FILE_SIZE / (1024 * 1024))
            );
        }
        
        // Validate each file
        for (int i = 0; i < files.size(); i++) {
            try {
                validateImageFile(files.get(i), true);
            } catch (InvalidImageException e) {
                throw new InvalidImageException(
                    String.format("File #%d validation failed: %s", i + 1, e.getMessage())
                );
            }
        }
    }
    
    /**
     * Quick check if file appears to be valid image (without full validation)
     */
    public boolean isValidImageFile(MultipartFile file) {
        try {
            validateImageFile(file);
            return true;
        } catch (InvalidImageException e) {
            return false;
        }
    }
}
