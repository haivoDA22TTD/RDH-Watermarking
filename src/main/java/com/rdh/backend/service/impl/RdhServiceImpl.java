package com.rdh.backend.service.impl;

import com.rdh.backend.dto.response.EvaluationResponse;
import com.rdh.backend.dto.response.ExtractResponse;
import com.rdh.backend.service.RdhService;
import com.rdh.backend.util.MetricsCalculator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
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

    // 8 pixel đầu tiên của hàng 0 dùng để lưu peak (8 bit)
    private static final int HEADER_PIXELS = 8;

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
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageFile.getBytes()));

        // Ép kiểu về TYPE_INT_RGB để tránh lỗi với ảnh indexed, grayscale, ARGB
        BufferedImage image = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        int width = image.getWidth();
        int height = image.getHeight();

        String dataToHide = secretData + "###";
        byte[] bytes = dataToHide.getBytes(StandardCharsets.UTF_8);

        // 1. Tìm Peak trên kênh Red (bỏ qua HEADER_PIXELS đầu tiên, range [1, 254])
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y == 0 && x < HEADER_PIXELS) continue;
                int red = (image.getRGB(x, y) >> 16) & 0xFF;
                histogram[red]++;
            }
        }

        int peak = 1;
        for (int i = 1; i <= 254; i++) {
            if (histogram[i] > histogram[peak]) {
                peak = i;
            }
        }
        
        System.out.println("EMBED - Peak found: " + peak + ", count: " + histogram[peak]);

        // 2. Ghi peak vào 8 pixel header TRƯỚC KHI shift (LSB của kênh Red)
        for (int i = 0; i < HEADER_PIXELS; i++) {
            int rgb = image.getRGB(i, 0);
            int red = (rgb >> 16) & 0xFF;
            int bit = (peak >> (7 - i)) & 1;
            red = (red & 0xFE) | bit;
            image.setRGB(i, 0, (rgb & 0xFF00FFFF) | (red << 16));
        }
        
        // Verify header was written correctly
        int verifyPeak = 0;
        for (int i = 0; i < HEADER_PIXELS; i++) {
            int red = (image.getRGB(i, 0) >> 16) & 0xFF;
            verifyPeak = (verifyPeak << 1) | (red & 1);
        }
        System.out.println("EMBED - Peak written to header: " + verifyPeak);

        // 3. Dịch chuyển Histogram (bỏ qua HEADER_PIXELS đầu tiên)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y == 0 && x < HEADER_PIXELS) continue;

                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;

                if (red > peak && red <= 254) {
                    red = red + 1;
                    int newRgb = (rgb & 0xFF00FFFF) | (red << 16);
                    image.setRGB(x, y, newRgb);
                }
            }
        }

        // 4. Nhúng tin tại vị trí Peak (bỏ qua HEADER_PIXELS đầu tiên)
        int bitIndex = 0;
        int totalBits = bytes.length * 8;

        for (int y = 0; y < height && bitIndex < totalBits; y++) {
            for (int x = 0; x < width && bitIndex < totalBits; x++) {
                if (y == 0 && x < HEADER_PIXELS) continue;

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
        
        System.out.println("EMBED - Total bits embedded: " + bitIndex + " / " + totalBits);
        if (bitIndex < totalBits) {
            System.out.println("WARNING: Not enough capacity! Need " + totalBits + " bits but only embedded " + bitIndex);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    @Override
    public ExtractResponse extractAndRestore(MultipartFile stegoImageFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(stegoImageFile.getBytes()));

        // Ép kiểu về TYPE_INT_RGB để đảm bảo đọc đúng giá trị Red channel
        BufferedImage image = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        int width = image.getWidth();
        int height = image.getHeight();

        // 1. Đọc peak từ 8 pixel header (LSB của kênh Red)
        int peak = 0;
        for (int i = 0; i < HEADER_PIXELS; i++) {
            int red = (image.getRGB(i, 0) >> 16) & 0xFF;
            peak = (peak << 1) | (red & 1);
        }
        
        System.out.println("EXTRACT - Peak read from header: " + peak);

        // 2. Giải mã dữ liệu (bỏ qua HEADER_PIXELS đầu tiên)
        StringBuilder bits = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y == 0 && x < HEADER_PIXELS) continue;

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
                            System.out.println("EXTRACT - Found delimiter! Data: " + resultData);
                            return ExtractResponse.builder()
                                    .extractedData(resultData)
                                    .build();
                        }
                    }
                }
            }
        }
        
        System.out.println("EXTRACT - No delimiter found. Total bits collected: " + bits.length());

        return ExtractResponse.builder()
                .extractedData("Không tìm thấy tin ẩn")
                .build();
    }

    @Override
    public EvaluationResponse evaluateImageQuality(MultipartFile originalImageFile, MultipartFile stegoImageFile) throws IOException {
        // Đọc và chuẩn hóa ảnh gốc
        BufferedImage originalTemp = ImageIO.read(new ByteArrayInputStream(originalImageFile.getBytes()));
        BufferedImage original = new BufferedImage(originalTemp.getWidth(), originalTemp.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = original.createGraphics();
        g1.drawImage(originalTemp, 0, 0, null);
        g1.dispose();

        // Đọc và chuẩn hóa ảnh stego
        BufferedImage stegoTemp = ImageIO.read(new ByteArrayInputStream(stegoImageFile.getBytes()));
        BufferedImage stego = new BufferedImage(stegoTemp.getWidth(), stegoTemp.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = stego.createGraphics();
        g2.drawImage(stegoTemp, 0, 0, null);
        g2.dispose();

        // Kiểm tra kích thước
        if (original.getWidth() != stego.getWidth() || original.getHeight() != stego.getHeight()) {
            return EvaluationResponse.builder()
                    .message("Lỗi: Ảnh gốc và ảnh stego phải có cùng kích thước")
                    .width(original.getWidth())
                    .height(original.getHeight())
                    .build();
        }

        try {
            // Tính các metrics
            double psnr = MetricsCalculator.calculatePSNR(original, stego);
            long capacity = MetricsCalculator.calculateCapacity(original);
            double ssim = MetricsCalculator.calculateSSIM(original, stego);

            System.out.println("EVALUATE - PSNR: " + psnr + " dB");
            System.out.println("EVALUATE - Capacity: " + capacity + " bits");
            System.out.println("EVALUATE - SSIM: " + ssim);

            return EvaluationResponse.builder()
                    .psnr(psnr)
                    .capacity(capacity)
                    .ssim(ssim)
                    .width(original.getWidth())
                    .height(original.getHeight())
                    .message("Đánh giá chất lượng thành công")
                    .build();

        } catch (Exception e) {
            System.err.println("EVALUATE - Error: " + e.getMessage());
            return EvaluationResponse.builder()
                    .message("Lỗi khi tính toán metrics: " + e.getMessage())
                    .width(original.getWidth())
                    .height(original.getHeight())
                    .build();
        }
    }
}
