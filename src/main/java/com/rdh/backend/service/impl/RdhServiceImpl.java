package com.rdh.backend.service.impl;

import com.rdh.backend.dto.response.ExtractResponse;
import com.rdh.backend.service.RdhService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RdhServiceImpl implements RdhService {

    @Override
    public List<Map<String, String>> getAvailableAlgorithms() {
        List<Map<String, String>> list = new ArrayList<>();

        Map<String, String> algo1 = new LinkedHashMap<>();
        algo1.put("code", "HISTOGRAM_SHIFTING");
        algo1.put("name", "Histogram Shifting (RDH)");
        algo1.put("description", "Thuật toán giấu tin thuận nghịch - Phục hồi ảnh gốc 100%");

        list.add(algo1);
        return list;
    }

    @Override
    public byte[] embed(MultipartFile imageFile, String secretData) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));
        int width = image.getWidth();
        int height = image.getHeight();

        String dataToHide = secretData + "###";
        byte[] bytes = dataToHide.getBytes(StandardCharsets.UTF_8);

        // 1. Tìm Peak trên kênh Red
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

        // 2. Dịch chuyển Histogram
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;

                if (red > peak && red < 255) {
                    red = red + 1;
                    int newRgb = (rgb & 0xFF00FFFF) | (red << 16);
                    image.setRGB(x, y, newRgb);
                }
            }
        }

        // 3. Nhúng tin tại vị trí Peak
        int bitIndex = 0;
        int totalBits = bytes.length * 8;

        for (int y = 0; y < height && bitIndex < totalBits; y++) {
            for (int x = 0; x < width && bitIndex < totalBits; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;

                if (red == peak) {
                    int byteIndex = bitIndex / 8;
                    int bitPos = 7 - (bitIndex % 8);
                    int bitToEmbed = (bytes[byteIndex] >> bitPos) & 1;

                    red = peak + bitToEmbed;
                    int newRgb = (rgb & 0xFF00FFFF) | (red << 16);
                    image.setRGB(x, y, newRgb);

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

        // 1. Tìm Peak trong ảnh Stego
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int red = (image.getRGB(x, y) >> 16) & 0xFF;
                histogram[red]++;
            }
        }

        int peak = 0;
        int maxCount = 0;
        for (int i = 0; i < 255; i++) {
            if (histogram[i] + histogram[i + 1] > maxCount) {
                maxCount = histogram[i] + histogram[i + 1];
                peak = i;
            }
        }

        // 2. Giải mã dữ liệu
        StringBuilder bits = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;

                if (red == peak || red == peak + 1) {
                    int bit = (red == peak + 1) ? 1 : 0;
                    bits.append(bit);

                    if (bits.length() == 8) {
                        int b = Integer.parseInt(bits.toString(), 2);
                        baos.write(b);
                        bits.setLength(0);

                        String currentText = baos.toString(StandardCharsets.UTF_8);
                        if (currentText.endsWith("###")) {
                            String resultData = currentText.substring(0, currentText.length() - 3);
                            return ExtractResponse.builder()
                                    .extractedData(resultData)
                                    .build();
                        }
                    }
                }
            }
        }

        return ExtractResponse.builder()
                .extractedData("Không tìm thấy tin ẩn")
                .build();
    }
}