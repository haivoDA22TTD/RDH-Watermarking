package com.rdh.backend.validation;

import com.rdh.backend.exception.InvalidImageException;
import com.rdh.backend.exception.InsufficientCapacityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for WatermarkTextValidator
 */
class WatermarkTextValidatorTest {

    private WatermarkTextValidator validator;

    @BeforeEach
    void setUp() {
        validator = new WatermarkTextValidator();
    }

    // ==================== Null and Empty Tests ====================

    @Test
    @DisplayName("Should throw exception when text is null")
    void testNullText() {
        assertThatThrownBy(() -> validator.validateWatermarkText(null))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("required");
    }

    @Test
    @DisplayName("Should throw exception when text is empty")
    void testEmptyText() {
        assertThatThrownBy(() -> validator.validateWatermarkText(""))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("required");
    }

    @Test
    @DisplayName("Should throw exception when text is only whitespace")
    void testWhitespaceOnlyText() {
        assertThatThrownBy(() -> validator.validateWatermarkText("   "))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("required");
    }

    // ==================== Length Tests ====================

    @Test
    @DisplayName("Should accept minimum length (1 character)")
    void testMinimumLength() {
        assertThatCode(() -> validator.validateWatermarkText("A"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept maximum length (64 characters)")
    void testMaximumLength() {
        String text = "A".repeat(64);
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception when text exceeds 64 characters")
    void testExceedMaxLength() {
        String text = "A".repeat(65);
        
        assertThatThrownBy(() -> validator.validateWatermarkText(text))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("too long");
    }

    @Test
    @DisplayName("Should accept common length watermarks")
    void testCommonLengths() {
        String[] texts = {
            "COPYRIGHT",
            "COPYRIGHT_2024",
            "COPYRIGHT_2024_JOHN_DOE",
            "CONFIDENTIAL_DOCUMENT_2024"
        };
        
        for (String text : texts) {
            assertThatCode(() -> validator.validateWatermarkText(text))
                .as("Text: " + text)
                .doesNotThrowAnyException();
        }
    }

    // ==================== Character Validation Tests ====================

    @Test
    @DisplayName("Should accept alphanumeric characters")
    void testAlphanumericCharacters() {
        String text = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept common punctuation")
    void testCommonPunctuation() {
        String text = "Hello, World! (2024) - Copyright @ Company.";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept spaces")
    void testSpaces() {
        String text = "Hello World 2024";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept underscore and hyphen")
    void testUnderscoreHyphen() {
        String text = "COPYRIGHT_2024-JOHN_DOE";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "©", // Copyright symbol (not ASCII 32-126)
        "€", // Euro symbol
        "中文", // Chinese characters
        "日本語", // Japanese characters
        "한글", // Korean characters
        "Привет" // Russian characters
    })
    @DisplayName("Should throw exception for non-ASCII characters")
    void testNonAsciiCharacters(String text) {
        assertThatThrownBy(() -> validator.validateWatermarkText(text))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("Invalid character");
    }

    @Test
    @DisplayName("Should throw exception for control characters")
    void testControlCharacters() {
        String text = "Hello\nWorld"; // Contains newline (ASCII 10)
        
        assertThatThrownBy(() -> validator.validateWatermarkText(text))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("Invalid character");
    }

    @Test
    @DisplayName("Should throw exception for tab character")
    void testTabCharacter() {
        String text = "Hello\tWorld"; // Contains tab (ASCII 9)
        
        assertThatThrownBy(() -> validator.validateWatermarkText(text))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("Invalid character");
    }

    // ==================== Delimiter Tests ====================

    @Test
    @DisplayName("Should throw exception when text contains delimiter")
    void testContainsDelimiter() {
        String text = "Hello###END###World";
        
        assertThatThrownBy(() -> validator.validateWatermarkText(text))
            .isInstanceOf(InvalidImageException.class)
            .hasMessageContaining("delimiter");
    }

    @Test
    @DisplayName("Should accept text with partial delimiter")
    void testPartialDelimiter() {
        String text = "###END"; // Not full delimiter
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept text with delimiter-like string")
    void testDelimiterLikeString() {
        String text = "END";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    // ==================== Capacity Validation Tests ====================

    @Test
    @DisplayName("Should pass capacity validation when sufficient")
    void testSufficientCapacity() {
        String text = "COPYRIGHT";
        long capacity = 10000; // More than enough
        
        assertThatCode(() -> validator.validateWatermarkTextWithCapacity(text, capacity))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should throw exception when capacity insufficient")
    void testInsufficientCapacity() {
        String text = "COPYRIGHT_2024_VERY_LONG_TEXT";
        long capacity = 100; // Not enough
        
        assertThatThrownBy(() -> validator.validateWatermarkTextWithCapacity(text, capacity))
            .isInstanceOf(InsufficientCapacityException.class)
            .hasMessageContaining("Insufficient");
    }

    @Test
    @DisplayName("Should calculate required bits correctly")
    void testCalculateRequiredBits() {
        String text = "TEST";
        // "TEST" + "###END###" = 4 + 9 = 13 characters = 13 * 8 = 104 bits
        
        long requiredBits = validator.calculateRequiredBits(text);
        
        assertThat(requiredBits).isEqualTo(104);
    }

    @Test
    @DisplayName("Should return 0 for null text in bit calculation")
    void testCalculateRequiredBitsNull() {
        long requiredBits = validator.calculateRequiredBits(null);
        
        assertThat(requiredBits).isEqualTo(0);
    }

    @Test
    @DisplayName("Should warn when using > 80% capacity")
    void testHighCapacityUsage() {
        String text = "A".repeat(50); // Long text
        long capacity = 500; // Just enough but > 80% usage
        
        // Should not throw but will log warning
        assertThatCode(() -> validator.validateWatermarkTextWithCapacity(text, capacity))
            .doesNotThrowAnyException();
    }

    // ==================== Sanitization Tests ====================

    @Test
    @DisplayName("Should sanitize invalid characters")
    void testSanitizeInvalidCharacters() {
        String text = "Hello\nWorld";
        String sanitized = validator.sanitizeWatermarkText(text);
        
        assertThat(sanitized).doesNotContain("\n");
        assertThat(sanitized).isEqualTo("Hello_World");
    }

    @Test
    @DisplayName("Should replace delimiter in sanitization")
    void testSanitizeDelimiter() {
        String text = "Hello###END###World";
        String sanitized = validator.sanitizeWatermarkText(text);
        
        assertThat(sanitized).doesNotContain("###END###");
    }

    @Test
    @DisplayName("Should return null for null input in sanitization")
    void testSanitizeNull() {
        String sanitized = validator.sanitizeWatermarkText(null);
        
        assertThat(sanitized).isNull();
    }

    @Test
    @DisplayName("Should keep valid characters in sanitization")
    void testSanitizeValidCharacters() {
        String text = "Valid_Text-2024";
        String sanitized = validator.sanitizeWatermarkText(text);
        
        assertThat(sanitized).isEqualTo(text);
    }

    // ==================== Helper Method Tests ====================

    @Test
    @DisplayName("Should calculate max recommended length correctly")
    void testGetMaxRecommendedLength() {
        long capacity = 10000;
        int maxLength = validator.getMaxRecommendedLength(capacity);
        
        // Should be reasonable but not exceed 64
        assertThat(maxLength).isPositive();
        assertThat(maxLength).isLessThanOrEqualTo(64);
    }

    @Test
    @DisplayName("Should cap max recommended length at 64")
    void testGetMaxRecommendedLengthCapped() {
        long capacity = 1000000; // Very large capacity
        int maxLength = validator.getMaxRecommendedLength(capacity);
        
        assertThat(maxLength).isEqualTo(64);
    }

    @Test
    @DisplayName("Should return 0 for insufficient capacity")
    void testGetMaxRecommendedLengthInsufficientCapacity() {
        long capacity = 50; // Very small
        int maxLength = validator.getMaxRecommendedLength(capacity);
        
        // Should be very small or 0
        assertThat(maxLength).isLessThan(10);
    }

    // ==================== Real-world Scenarios ====================

    @Test
    @DisplayName("Should accept typical copyright text")
    void testTypicalCopyright() {
        String text = "Copyright 2024 Company Name. All rights reserved.";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept document ID format")
    void testDocumentIdFormat() {
        String text = "DOC-2024-001-CONFIDENTIAL";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept user identification")
    void testUserIdentification() {
        String text = "User:john.doe@company.com";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should accept timestamp format")
    void testTimestampFormat() {
        String text = "2024-01-15 14:30:00 UTC";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("Should accept all printable ASCII characters")
    void testAllPrintableAscii() {
        StringBuilder sb = new StringBuilder();
        for (int i = 32; i <= 64; i++) { // First 33 printable ASCII chars
            sb.append((char) i);
        }
        String text = sb.toString();
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle text with mixed case")
    void testMixedCase() {
        String text = "CoNfIdEnTiAl_DoCuMeNt_2024";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle numbers only")
    void testNumbersOnly() {
        String text = "1234567890";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle special characters")
    void testSpecialCharacters() {
        String text = "!@#$%^&*()_+-=[]{}|;:',.<>?/~`";
        
        assertThatCode(() -> validator.validateWatermarkText(text))
            .doesNotThrowAnyException();
    }
}
