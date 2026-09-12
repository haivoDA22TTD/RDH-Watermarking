package com.rdh.backend.util;

import java.awt.image.BufferedImage;

/**
 * Utility class để tính toán các metrics đánh giá chất lượng ảnh RDH
 * - PSNR (Peak Signal-to-Noise Ratio)
 * - Capacity (Dung lượng nhúng)
 * - SSIM (Structural Similarity Index)
 */
public class MetricsCalculator {

    private static final int HEADER_PIXELS = 8;

    /**
     * Tính PSNR giữa ảnh gốc và ảnh stego
     * PSNR = 10 * log10(MAX^2 / MSE)
     * Trong đó MAX = 255 (giá trị pixel tối đa cho ảnh 8-bit)
     * 
     * @param original Ảnh gốc
     * @param stego Ảnh stego
     * @return PSNR tính bằng dB, hoặc Double.POSITIVE_INFINITY nếu ảnh giống hệt
     */
    public static double calculatePSNR(BufferedImage original, BufferedImage stego) {
        if (original.getWidth() != stego.getWidth() || original.getHeight() != stego.getHeight()) {
            throw new IllegalArgumentException("Ảnh gốc và ảnh stego phải có cùng kích thước");
        }

        int width = original.getWidth();
        int height = original.getHeight();

        // Tính MSE (Mean Square Error)
        double mse = 0.0;
        int totalPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgbOriginal = original.getRGB(x, y);
                int rgbStego = stego.getRGB(x, y);

                // Tách các kênh màu
                int rOriginal = (rgbOriginal >> 16) & 0xFF;
                int gOriginal = (rgbOriginal >> 8) & 0xFF;
                int bOriginal = rgbOriginal & 0xFF;

                int rStego = (rgbStego >> 16) & 0xFF;
                int gStego = (rgbStego >> 8) & 0xFF;
                int bStego = rgbStego & 0xFF;

                // Tính sai số bình phương cho từng kênh
                double diffR = rOriginal - rStego;
                double diffG = gOriginal - gStego;
                double diffB = bOriginal - bStego;

                mse += (diffR * diffR + diffG * diffG + diffB * diffB) / 3.0;
            }
        }

        mse /= totalPixels;

        // Nếu MSE = 0 nghĩa là ảnh giống hệt nhau
        if (mse == 0) {
            return Double.POSITIVE_INFINITY;
        }

        // PSNR = 10 * log10(MAX^2 / MSE)
        double maxPixelValue = 255.0;
        return 10.0 * Math.log10((maxPixelValue * maxPixelValue) / mse);
    }

    /**
     * Tính Capacity - Số bit tối đa có thể nhúng vào ảnh
     * Với thuật toán Histogram Shifting, capacity phụ thuộc vào số lượng pixel có giá trị = peak
     * 
     * @param image Ảnh cần tính capacity
     * @return Số bit tối đa có thể nhúng
     */
    public static long calculateCapacity(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Tìm histogram của kênh Red (bỏ qua header pixels)
        int[] histogram = new int[256];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y == 0 && x < HEADER_PIXELS) continue;
                
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                histogram[red]++;
            }
        }

        // Tìm peak trong range [1, 254]
        int peak = 1;
        for (int i = 1; i <= 254; i++) {
            if (histogram[i] > histogram[peak]) {
                peak = i;
            }
        }

        // Capacity = số pixel có giá trị Red = peak
        // Mỗi pixel peak có thể nhúng 1 bit
        return histogram[peak];
    }

    /**
     * Tính SSIM (Structural Similarity Index) giữa ảnh gốc và ảnh stego
     * SSIM đánh giá độ tương đồng cấu trúc thị giác, tốt hơn PSNR
     * 
     * SSIM(x,y) = [l(x,y)]^α * [c(x,y)]^β * [s(x,y)]^γ
     * Trong đó:
     * - l(x,y): luminance comparison (so sánh độ sáng)
     * - c(x,y): contrast comparison (so sánh độ tương phản)
     * - s(x,y): structure comparison (so sánh cấu trúc)
     * 
     * Công thức đơn giản hóa (α = β = γ = 1):
     * SSIM = (2μxμy + C1)(2σxy + C2) / [(μx² + μy² + C1)(σx² + σy² + C2)]
     * 
     * @param original Ảnh gốc
     * @param stego Ảnh stego
     * @return SSIM trong khoảng [0, 1], 1.0 = giống hệt
     */
    public static double calculateSSIM(BufferedImage original, BufferedImage stego) {
        if (original.getWidth() != stego.getWidth() || original.getHeight() != stego.getHeight()) {
            throw new IllegalArgumentException("Ảnh gốc và ảnh stego phải có cùng kích thước");
        }

        int width = original.getWidth();
        int height = original.getHeight();

        // Constants để tránh chia cho 0
        double C1 = 6.5025;  // (0.01 * 255)^2
        double C2 = 58.5225; // (0.03 * 255)^2

        // Tính mean (μ) và variance (σ²) cho từng ảnh
        double meanOriginal = 0.0;
        double meanStego = 0.0;
        int totalPixels = width * height;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                meanOriginal += getGrayscale(original.getRGB(x, y));
                meanStego += getGrayscale(stego.getRGB(x, y));
            }
        }

        meanOriginal /= totalPixels;
        meanStego /= totalPixels;

        // Tính variance và covariance
        double varianceOriginal = 0.0;
        double varianceStego = 0.0;
        double covariance = 0.0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double grayOriginal = getGrayscale(original.getRGB(x, y));
                double grayStego = getGrayscale(stego.getRGB(x, y));

                double diffOriginal = grayOriginal - meanOriginal;
                double diffStego = grayStego - meanStego;

                varianceOriginal += diffOriginal * diffOriginal;
                varianceStego += diffStego * diffStego;
                covariance += diffOriginal * diffStego;
            }
        }

        varianceOriginal /= (totalPixels - 1);
        varianceStego /= (totalPixels - 1);
        covariance /= (totalPixels - 1);

        // Tính SSIM
        double numerator = (2 * meanOriginal * meanStego + C1) * (2 * covariance + C2);
        double denominator = (meanOriginal * meanOriginal + meanStego * meanStego + C1) *
                             (varianceOriginal + varianceStego + C2);

        return numerator / denominator;
    }

    /**
     * Chuyển đổi RGB sang grayscale
     * Gray = 0.299*R + 0.587*G + 0.114*B (ITU-R BT.601)
     */
    private static double getGrayscale(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0.299 * r + 0.587 * g + 0.114 * b;
    }
}
