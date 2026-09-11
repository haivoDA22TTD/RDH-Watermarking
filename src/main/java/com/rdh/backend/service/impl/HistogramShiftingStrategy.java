package com.rdh.backend.service.impl;

import com.rdh.backend.dto.enumtype.RdhAlgorithmType;
import com.rdh.backend.dto.response.ExtractResponse;
import com.rdh.backend.service.RdhStrategy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class HistogramShiftingStrategy implements RdhStrategy {

    private static final String DELIMITER = "###";
    private static final int HEADER_PIXELS = 16;

    @Override
    public RdhAlgorithmType getAlgorithmType() {
        return RdhAlgorithmType.HISTOGRAM_SHIFTING;
    }

    @Override
    public byte[] embed(MultipartFile imageFile, String secretData) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

        // Cốt lõi: Ép kiểu ảnh về RGB 24-bit chuẩn để ImageIO không tự động biến đổi màu khi lưu file
        BufferedImage image = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        int width = image.getWidth();
        int height = image.getHeight();

        String dataToHide = secretData + DELIMITER;
        byte[] bytes = dataToHide.getBytes(StandardCharsets.UTF_8);
        int totalBits = bytes.length * 8;

        // 1. Tìm Peak trên kênh Red trong vùng [2, 252]
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y == 0 && x < HEADER_PIXELS) continue;
                int red = (image.getRGB(x, y) >> 16) & 0xFF;
                histogram[red]++;
            }
        }

        int peak = 2;
        for (int i = 2; i <= 252; i++) {
            if (histogram[i] > histogram[peak]) {
                peak = i;
            }
        }

        // 2. Dịch chuyển Histogram an toàn
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y == 0 && x < HEADER_PIXELS) continue;

                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;

                if (red > peak && red < 254) {
                    red++;
                    image.setRGB(x, y, (rgb & 0xFF00FFFF) | (red << 16));
                }
            }
        }

        // 3. Nhúng dữ liệu
        int bitIndex = 0;
        for (int y = 0; y < height && bitIndex < totalBits; y++) {
            for (int x = 0; x < width && bitIndex < totalBits; x++) {
                if (y == 0 && x < HEADER_PIXELS) continue;

                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;

                if (red == peak) {
                    int bitToEmbed = (bytes[bitIndex / 8] >> (7 - (bitIndex % 8))) & 1;
                    red = peak + bitToEmbed;
                    image.setRGB(x, y, (rgb & 0xFF00FFFF) | (red << 16));
                    bitIndex++;
                }
            }
        }

        if (bitIndex < totalBits) {
            throw new IllegalArgumentException("Ảnh quá nhỏ không đủ dung lượng giấu tin. Vui lòng chọn ảnh lớn hơn.");
        }

        // 4. Ghi Header Metadata vào 16 pixel đầu tiên
        int payloadLength = bytes.length;
        int headerData = ((peak & 0xFF) << 8) | (payloadLength & 0xFF);

        for (int i = 0; i < HEADER_PIXELS; i++) {
            int rgb = image.getRGB(i, 0);
            int red = (rgb >> 16) & 0xFF;
            int bit = (headerData >> (15 - i)) & 1;
            red = (red & 0xFE) | bit;
            image.setRGB(i, 0, (rgb & 0xFF00FFFF) | (red << 16));
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    @Override
    public ExtractResponse extractAndRestore(MultipartFile stegoImageFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(stegoImageFile.getBytes()));

        // Chuẩn hóa màu khi giải mã
        BufferedImage image = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        int width = image.getWidth();
        int height = image.getHeight();

        // 1. Đọc Header
        int headerData = 0;
        for (int i = 0; i < HEADER_PIXELS; i++) {
            int red = (image.getRGB(i, 0) >> 16) & 0xFF;
            headerData = (headerData << 1) | (red & 1);
        }

        int peak = (headerData >> 8) & 0xFF;
        int targetBytesLength = headerData & 0xFF;
        int targetBitsLength = targetBytesLength * 8;

        // 2. Trích xuất
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int currentByte = 0;
        int bitCount = 0;
        int totalExtractedBits = 0;

        for (int y = 0; y < height && totalExtractedBits < targetBitsLength; y++) {
            for (int x = 0; x < width && totalExtractedBits < targetBitsLength; x++) {
                if (y == 0 && x < HEADER_PIXELS) continue;

                int red = (image.getRGB(x, y) >> 16) & 0xFF;

                if (red == peak || red == peak + 1) {
                    int bit = (red == peak + 1) ? 1 : 0;
                    currentByte = (currentByte << 1) | bit;
                    bitCount++;
                    totalExtractedBits++;

                    if (bitCount == 8) {
                        buffer.write(currentByte);
                        currentByte = 0;
                        bitCount = 0;
                    }
                }
            }
        }

        byte[] extractedBytes = buffer.toByteArray();
        String fullText = new String(extractedBytes, StandardCharsets.UTF_8);

        if (fullText.endsWith(DELIMITER)) {
            String cleanData = fullText.substring(0, fullText.length() - DELIMITER.length());
            return ExtractResponse.builder().extractedData(cleanData).build();
        }

        return ExtractResponse.builder().extractedData("Không tìm thấy tin ẩn").build();
    }
}