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

@Service
public class RdhServiceImpl implements RdhService {

    @Override
    public byte[] embed(MultipartFile imageFile, String secretData) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

        String dataToHide = secretData + "###";
        byte[] bytes = dataToHide.getBytes(StandardCharsets.UTF_8);

        int width = image.getWidth();
        int height = image.getHeight();
        int bitIndex = 0;
        int totalBits = bytes.length * 8;

        for (int y = 0; y < height && bitIndex < totalBits; y++) {
            for (int x = 0; x < width && bitIndex < totalBits; x++) {
                int rgb = image.getRGB(x, y);

                int byteIndex = bitIndex / 8;
                int bitPos = 7 - (bitIndex % 8);
                int bitToEmbed = (bytes[byteIndex] >> bitPos) & 1;

                int red = (rgb >> 16) & 0xFF;
                red = (red & 0xFE) | bitToEmbed;

                int newRgb = (rgb & 0xFF00FFFF) | (red << 16);
                image.setRGB(x, y, newRgb);

                bitIndex++;
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

        StringBuilder bits = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;

                int lsb = red & 1;
                bits.append(lsb);

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

        return ExtractResponse.builder()
                .extractedData("Không tìm thấy tin ẩn")
                .build();
    }
}