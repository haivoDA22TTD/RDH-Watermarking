package com.rdh.backend.exception;

/**
 * Exception thrown when image doesn't have enough capacity for watermark/data
 * Occurs when data to embed is larger than available capacity
 */
public class InsufficientCapacityException extends RdhException {
    
    private final long requiredCapacity;
    private final long availableCapacity;
    
    public InsufficientCapacityException(String message) {
        super(message);
        this.requiredCapacity = -1;
        this.availableCapacity = -1;
    }
    
    public InsufficientCapacityException(long requiredCapacity, long availableCapacity) {
        super(formatMessage(requiredCapacity, availableCapacity));
        this.requiredCapacity = requiredCapacity;
        this.availableCapacity = availableCapacity;
    }
    
    public InsufficientCapacityException(String message, long requiredCapacity, long availableCapacity) {
        super(message);
        this.requiredCapacity = requiredCapacity;
        this.availableCapacity = availableCapacity;
    }
    
    private static String formatMessage(long required, long available) {
        return String.format(
            "Insufficient capacity: Required %d bits, but only %d bits available. " +
            "Deficit: %d bits (%.1f%% short). " +
            "Suggestions: Use larger image, shorter watermark text, or reduce embedding strength.",
            required, available, required - available, 
            ((required - available) * 100.0 / required)
        );
    }
    
    public long getRequiredCapacity() {
        return requiredCapacity;
    }
    
    public long getAvailableCapacity() {
        return availableCapacity;
    }
    
    public long getDeficit() {
        return requiredCapacity - availableCapacity;
    }
    
    /**
     * Create exception with suggestions
     */
    public static InsufficientCapacityException withSuggestions(
            long required, long available, String... suggestions) {
        StringBuilder message = new StringBuilder(formatMessage(required, available));
        if (suggestions != null && suggestions.length > 0) {
            message.append("\nSuggested solutions:");
            for (int i = 0; i < suggestions.length; i++) {
                message.append(String.format("\n%d. %s", i + 1, suggestions[i]));
            }
        }
        return new InsufficientCapacityException(message.toString(), required, available);
    }
}
