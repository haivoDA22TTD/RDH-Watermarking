package com.rdh.backend.service.impl;

import com.rdh.backend.dto.request.TextStegoRequest;
import com.rdh.backend.dto.response.TextStegoResponse;
import com.rdh.backend.exception.InvalidImageException;
import com.rdh.backend.exception.WatermarkNotFoundException;
import com.rdh.backend.service.TextStegoService;
import com.rdh.backend.strategy.textStego.UnicodeZeroWidthStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of TextStegoService using Unicode Zero-Width Character strategy.
 * 
 * @author RDH Backend Team
 * @version 1.0
 * @since 2026-11-08
 */
@Service
public class TextStegoServiceImpl implements TextStegoService {
    
    private static final Logger log = LoggerFactory.getLogger(TextStegoServiceImpl.class);
    
    private final UnicodeZeroWidthStrategy strategy;
    
    public TextStegoServiceImpl(UnicodeZeroWidthStrategy strategy) {
        this.strategy = strategy;
    }
    
    @Override
    public TextStegoResponse embed(TextStegoRequest request) {
        log.info("Embedding secret data into text (cover text length: {}, secret length: {})",
                request.getCoverText().length(), request.getSecretData().length());
        
        try {
            // Validate inputs
            validateEmbedRequest(request);
            
            // Calculate capacity
            long capacity = strategy.calculateCapacity(request.getCoverText());
            long required = (long) request.getSecretData().length();
            
            log.debug("Capacity: {} bytes, Required: {} bytes", capacity, required);
            
            // Embed
            long startTime = System.currentTimeMillis();
            String stegoText = strategy.embed(request.getCoverText(), request.getSecretData());
            long duration = System.currentTimeMillis() - startTime;
            
            // Build response
            TextStegoResponse response = new TextStegoResponse();
            response.setSuccess(true);
            response.setMessage("Secret data embedded successfully");
            response.setStegoText(stegoText);
            response.setSecretData(null); // Don't return secret on embed
            
            // Metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("algorithm", strategy.getAlgorithmName());
            metadata.put("coverTextLength", request.getCoverText().length());
            metadata.put("stegoTextLength", stegoText.length());
            metadata.put("secretDataLength", request.getSecretData().length());
            metadata.put("capacity", capacity);
            metadata.put("usage", String.format("%.1f%%", (required * 100.0 / capacity)));
            metadata.put("processingTime", duration + "ms");
            metadata.put("format", detectFormat(request.getCoverText()));
            
            response.setMetadata(metadata);
            
            log.info("Embed successful. Stego text length: {}, Duration: {}ms",
                    stegoText.length(), duration);
            
            return response;
            
        } catch (IllegalArgumentException e) {
            log.error("Embed failed: {}", e.getMessage());
            throw new InvalidImageException("Text steganography embed failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during embed", e);
            throw new RuntimeException("Failed to embed secret data: " + e.getMessage(), e);
        }
    }
    
    @Override
    public TextStegoResponse extract(String stegoText) {
        log.info("Extracting secret data from text (length: {})", stegoText.length());
        
        try {
            // Validate
            if (stegoText == null || stegoText.trim().isEmpty()) {
                throw new InvalidImageException("Stego text cannot be null or empty");
            }
            
            // Extract
            long startTime = System.currentTimeMillis();
            String secretData = strategy.extract(stegoText);
            long duration = System.currentTimeMillis() - startTime;
            
            // Build response
            TextStegoResponse response = new TextStegoResponse();
            response.setSuccess(true);
            response.setMessage("Secret data extracted successfully");
            response.setStegoText(null); // Don't return stego text
            response.setSecretData(secretData);
            
            // Metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("algorithm", strategy.getAlgorithmName());
            metadata.put("stegoTextLength", stegoText.length());
            metadata.put("extractedLength", secretData.length());
            metadata.put("processingTime", duration + "ms");
            metadata.put("format", detectFormat(stegoText));
            
            response.setMetadata(metadata);
            
            log.info("Extract successful. Secret data length: {}, Duration: {}ms",
                    secretData.length(), duration);
            
            return response;
            
        } catch (WatermarkNotFoundException e) {
            log.warn("No hidden data found: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Extract failed: {}", e.getMessage());
            throw new InvalidImageException("Text steganography extract failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during extract", e);
            throw new RuntimeException("Failed to extract secret data: " + e.getMessage(), e);
        }
    }
    
    @Override
    public TextStegoResponse detect(String text) {
        log.info("Detecting hidden data in text (length: {})", text.length());
        
        try {
            boolean hasHiddenData = strategy.hasHiddenData(text);
            
            TextStegoResponse response = new TextStegoResponse();
            response.setSuccess(true);
            response.setMessage(hasHiddenData ? 
                "Hidden data detected" : "No hidden data detected");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("hasHiddenData", hasHiddenData);
            metadata.put("textLength", text.length());
            metadata.put("format", detectFormat(text));
            
            if (hasHiddenData) {
                metadata.put("statistics", strategy.getStatistics(text));
            }
            
            response.setMetadata(metadata);
            
            return response;
            
        } catch (Exception e) {
            log.error("Detection failed", e);
            throw new RuntimeException("Failed to detect hidden data: " + e.getMessage(), e);
        }
    }
    
    @Override
    public TextStegoResponse calculateCapacity(String coverText) {
        log.info("Calculating capacity for text (length: {})", coverText.length());
        
        try {
            long capacityBytes = strategy.calculateCapacity(coverText);
            long capacityBits = capacityBytes * 8;
            
            TextStegoResponse response = new TextStegoResponse();
            response.setSuccess(true);
            response.setMessage("Capacity calculated successfully");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("coverTextLength", coverText.length());
            metadata.put("capacityBytes", capacityBytes);
            metadata.put("capacityBits", capacityBits);
            metadata.put("maxSecretLength", capacityBytes);
            metadata.put("format", detectFormat(coverText));
            
            // Provide recommendations
            String recommendation;
            if (capacityBytes < 10) {
                recommendation = "Very low capacity. Use longer text for better results.";
            } else if (capacityBytes < 50) {
                recommendation = "Low capacity. Suitable for short messages only.";
            } else if (capacityBytes < 200) {
                recommendation = "Moderate capacity. Good for most use cases.";
            } else {
                recommendation = "High capacity. Excellent for larger secret data.";
            }
            
            metadata.put("recommendation", recommendation);
            
            response.setMetadata(metadata);
            
            return response;
            
        } catch (Exception e) {
            log.error("Capacity calculation failed", e);
            throw new RuntimeException("Failed to calculate capacity: " + e.getMessage(), e);
        }
    }
    
    @Override
    public TextStegoResponse clean(String stegoText) {
        log.info("Cleaning hidden data from text (length: {})", stegoText.length());
        
        try {
            String cleanText = strategy.removeHiddenData(stegoText);
            
            TextStegoResponse response = new TextStegoResponse();
            response.setSuccess(true);
            response.setMessage("Hidden data removed successfully");
            response.setStegoText(cleanText);
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("originalLength", stegoText.length());
            metadata.put("cleanedLength", cleanText.length());
            metadata.put("removedCharacters", stegoText.length() - cleanText.length());
            
            response.setMetadata(metadata);
            
            log.info("Clean successful. Removed {} characters",
                    stegoText.length() - cleanText.length());
            
            return response;
            
        } catch (Exception e) {
            log.error("Clean failed", e);
            throw new RuntimeException("Failed to clean text: " + e.getMessage(), e);
        }
    }
    
    @Override
    public TextStegoResponse analyze(String stegoText) {
        log.info("Analyzing text (length: {})", stegoText.length());
        
        try {
            boolean hasHiddenData = strategy.hasHiddenData(stegoText);
            String statistics = strategy.getStatistics(stegoText);
            
            TextStegoResponse response = new TextStegoResponse();
            response.setSuccess(true);
            response.setMessage("Analysis completed");
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("textLength", stegoText.length());
            metadata.put("hasHiddenData", hasHiddenData);
            metadata.put("statistics", statistics);
            metadata.put("format", detectFormat(stegoText));
            metadata.put("algorithm", strategy.getAlgorithmName());
            
            if (hasHiddenData) {
                try {
                    String extracted = strategy.extract(stegoText);
                    metadata.put("extractedLength", extracted.length());
                    metadata.put("canExtract", true);
                } catch (Exception e) {
                    metadata.put("canExtract", false);
                    metadata.put("extractionError", e.getMessage());
                }
            }
            
            response.setMetadata(metadata);
            
            return response;
            
        } catch (Exception e) {
            log.error("Analysis failed", e);
            throw new RuntimeException("Failed to analyze text: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate embed request.
     */
    private void validateEmbedRequest(TextStegoRequest request) {
        if (request == null) {
            throw new InvalidImageException("Request cannot be null");
        }
        
        if (request.getCoverText() == null || request.getCoverText().trim().isEmpty()) {
            throw new InvalidImageException("Cover text cannot be null or empty");
        }
        
        if (request.getSecretData() == null || request.getSecretData().trim().isEmpty()) {
            throw new InvalidImageException("Secret data cannot be null or empty");
        }
        
        // Check minimum cover text length
        if (request.getCoverText().length() < 10) {
            throw new InvalidImageException(
                "Cover text too short. Minimum 10 characters required, got: " + 
                request.getCoverText().length()
            );
        }
        
        // Check maximum secret data length
        if (request.getSecretData().length() > 10000) {
            throw new InvalidImageException(
                "Secret data too long. Maximum 10000 characters allowed, got: " + 
                request.getSecretData().length()
            );
        }
    }
    
    /**
     * Detect text format based on content.
     */
    private String detectFormat(String text) {
        if (text == null || text.isEmpty()) {
            return "unknown";
        }
        
        String trimmed = text.trim();
        
        // JSON
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return "json";
        }
        
        // XML/HTML
        if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            if (trimmed.toLowerCase().contains("<!doctype html") || 
                trimmed.toLowerCase().contains("<html")) {
                return "html";
            }
            return "xml";
        }
        
        // Markdown
        if (text.contains("# ") || text.contains("## ") || 
            text.contains("```") || text.contains("**")) {
            return "markdown";
        }
        
        // Java code
        if (text.contains("public class") || text.contains("import java.")) {
            return "java";
        }
        
        // Python code
        if (text.contains("def ") || text.contains("import ") || text.contains("class ")) {
            if (text.contains("print(") || text.contains("__init__")) {
                return "python";
            }
        }
        
        // JavaScript
        if (text.contains("function ") || text.contains("const ") || 
            text.contains("let ") || text.contains("var ")) {
            return "javascript";
        }
        
        // CSV
        if (text.contains(",") && text.split("\n").length > 1) {
            String[] lines = text.split("\n");
            if (lines.length > 0) {
                int commas = lines[0].split(",").length;
                if (commas > 1) {
                    return "csv";
                }
            }
        }
        
        return "plain-text";
    }
}
