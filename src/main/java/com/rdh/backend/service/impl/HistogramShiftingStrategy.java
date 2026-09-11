package com.rdh.backend.service.impl;

import com.rdh.backend.dto.enumtype.RdhAlgorithmType;
import com.rdh.backend.dto.response.ExtractResponse;
import com.rdh.backend.service.RdhStrategy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class HistogramShiftingStrategy implements RdhStrategy {

    private static final String DELIMITER = "###";

    @Override
    public RdhAlgorithmType getAlgorithmType() {
        return RdhAlgorithmType.HISTOGRAM_SHIFTING;
    }

    @Override
    public byte[] embed(MultipartFile imageFile, String secretData) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
        int width = image.getWidth();
        int height = image.getHeight();

        String dataToHide = secretData + DELIMITER;
        byte[] bytes = dataToHide.getBytes(StandardCharsets.UTF_8);
        int totalBits = bytes.length * 8;

        // 1. Tìm Peak trong kênh Red
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = (image.getRGB(x, y) >> 16) & 0xFF;
                histogram[red]++;
            }
        }

        int peak = 0;
        for (int i = 0; i < 256; i++) {
            if (histogram[i] > histogram[peak]) {
                peak = i;
            }
        }

        // 2. Shift các pixel > peak
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                if (red > peak && red < 255) {
                    red++;
                    image.setRGB(x, y, (rgb & 0xFF00FFFF) | (red << 16));
                }
            }
        }

        // 3. Nhúng bit vào vị trí Peak
        int bitIndex = 0;
        for (int y = 0; y < height && bitIndex < totalBits; y++) {
            for (int x = 0; x < width && bitIndex < totalBits; x++) {
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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    @Override
    public ExtractResponse extractAndRestore(MultipartFile stegoImageFile) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(stegoImageFile.getBytes()));
        int width = image.getWidth();
        int height = image.getHeight();

        // 1. Quét tìm lại cặp Peak & Peak + 1
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = (image.getRGB(x, y) >> 16) & 0xFF;
                histogram[red]++;
            }
        }

        int peak = 0;
        int maxCount = -1;
        for (int i = 0; i < 255; i++) {
            int count = histogram[i] + histogram[i + 1];
            if (count > maxCount) {
                maxCount = count;
                peak = i;
            }
        }

        // 2. Giải mã byte
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int currentByte = 0;
        int bitCount = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = (image.getRGB(x, y) >> 16) & 0xFF;

                if (red == peak || red == peak + 1) {
                    int bit = (red == peak + 1) ? 1 : 0;
                    currentByte = (currentByte << 1) | bit;
                    bitCount++;

                    if (bitCount == 8) {
                        buffer.write(currentByte);
                        currentByte = 0;
                        bitCount = 0;

                        byte[] data = buffer.toByteArray();
                        if (data.length >= 3 &&
                                data[data.length - 3] == '#' &&
                                data[data.length - 2] == '#' &&
                                data[data.length - 1] == '#') {

                            String result = new String(data, 0, data.length - 3, StandardCharsets.UTF_8);
                            return ExtractResponse.builder().extractedData(result).build();
                        }
                    }
                }
            }
        }

        return ExtractResponse.builder().extractedData("Không tìm thấy tin ẩn").build();
    }
}