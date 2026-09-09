package com.rdh.backend;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
public class RdhService {

    // 1. Thuật toán Nhúng tin thật vào Pixel của ảnh
    public byte[] embed(MultipartFile imageFile, String secretData) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

        // Thêm ký tự kết thúc "###" để nhận biết điểm dừng khi giải mã
        String dataToHide = secretData + "###";
        byte[] bytes = dataToHide.getBytes(StandardCharsets.UTF_8);

        int width = image.getWidth();
        int height = image.getHeight();
        int bitIndex = 0;
        int totalBits = bytes.length * 8;

        for (int y = 0; y < height && bitIndex < totalBits; y++) {
            for (int x = 0; x < width && bitIndex < totalBits; x++) {
                int rgb = image.getRGB(x, y);

                // Trích xuất bit cần giấu (0 hoặc 1)
                int byteIndex = bitIndex / 8;
                int bitPos = 7 - (bitIndex % 8);
                int bitToEmbed = (bytes[byteIndex] >> bitPos) & 1;

                // Giấu bit vào kênh Red (hoặc Blue) của Pixel
                int red = (rgb >> 16) & 0xFF;
                red = (red & 0xFE) | bitToEmbed; // Thay đổi bit LSB

                int newRgb = (rgb & 0xFF00FFFF) | (red << 16);
                image.setRGB(x, y, newRgb);

                bitIndex++;
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    // 2. Thuật toán Trích xuất tin thật từ Pixel của ảnh
    public Map<String, String> extractAndRestore(MultipartFile stegoImageFile) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(stegoImageFile.getBytes()));

        int width = image.getWidth();
        int height = image.getHeight();

        StringBuilder bits = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;

                // Lấy bit LSB từ kênh Red
                int lsb = red & 1;
                bits.append(lsb);

                // Khi gom đủ 8 bit (1 byte)
                if (bits.length() == 8) {
                    int b = Integer.parseInt(bits.toString(), 2);
                    baos.write(b);
                    bits.setLength(0); // Reset buffer bit

                    // Kiểm tra xem đã đọc tới chuỗi kết thúc "###" chưa
                    String currentText = baos.toString(StandardCharsets.UTF_8);
                    if (currentText.endsWith("###")) {
                        String resultData = currentText.substring(0, currentText.length() - 3);

                        Map<String, String> result = new HashMap<>();
                        result.put("status", "success");
                        result.put("extractedData", resultData);
                        return result;
                    }
                }
            }
        }

        Map<String, String> result = new HashMap<>();
        result.put("status", "error");
        result.put("extractedData", "Không tìm thấy tin ẩn!");
        return result;
    }
}