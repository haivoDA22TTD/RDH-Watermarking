# 🛡️ Reversible Data Hiding (RDH) Backend Service

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue.svg)](https://www.docker.com/)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20Layered-purple.svg)]()

Hệ thống RESTful API Backend chuyên dụng cho bài toán **Giấu tin thuận nghịch (Reversible Data Hiding - RDH)** trên hình ảnh kỹ thuật số. Dự án cho phép nhúng dữ liệu ẩn vào ảnh và trích xuất lại thông tin ban đầu, đồng thời đảm bảo khả năng **khôi phục 100% ảnh gốc** không bị vỡ hay mất mát dữ liệu.

---

## 🌟 Các tính năng chính (Key Features)

* **🔄 Giấu tin thuận nghịch (Pure RDH):** Ứng dụng thuật toán *Histogram Shifting* cho phép trích xuất tin nhắn và hoàn trả ảnh nguyên bản 100%.
* **🔌 Kiến trúc mở (Strategy Pattern):** Dễ dàng mở rộng, linh hoạt chuyển đổi giữa nhiều thuật toán giấu tin (*Histogram Shifting*, *LSB*,...) qua Query Parameter ở runtime.
* **📊 Đánh giá chất lượng ảnh (Evaluation Metrics):** Hỗ trợ tính toán chỉ số **PSNR** (Peak Signal-to-Noise Ratio) và **MSE** để đo lường độ nhiễu ảnh.
* **⚡ Tối ưu hiệu năng:** Xử lý dữ liệu nhị phân ở cấp độ Byte Array (`ByteArrayOutputStream`), thời gian phản hồi cực nhanh (< 100ms).
* **🐳 Triển khai dễ dàng:** Đóng gói ứng dụng sẵn sàng với Docker & Docker Compose (Multi-stage build siêu nhẹ).

---

## 🏗️ Kiến trúc dự án (Project Architecture)

Dự án áp dụng mô hình **Clean / Layered Architecture** để đảm bảo tính lỏng lẻo trong liên kết (Loose Coupling) và dễ bảo trì:

```text
com.rdh.backend/
├── ⚙️ config/                      # Cấu hình CORS, System Beans
├── 🎮 controller/                  # Tiếp nhận REST Request từ Client
├── 📦 dto/                         # Dữ liệu truyền tải (Request/Response)
│   ├── enumtype/                  # Quản lý Enum thuật toán (RdhAlgorithmType)
│   ├── request/                   # EmbedRequest, ExtractRequest
│   └── response/                  # ApiResponse<T>, ExtractResponse, EvaluationResponse
├── 🚨 exception/                   # Xử lý lỗi tập trung (Global Exception Handler)
└── 🧠 service/                     # Tầng nghiệp vụ cốt lõi
    ├── RdhService.java             # Service Manager (Factory Interface)
    ├── RdhStrategy.java            # Interface Strategy cho các thuật toán
    └── impl/
        ├── HistogramShiftingStrategy.java  # Algorithm 1: Histogram Shifting (RDH)
        ├── LsbStrategy.java                # Algorithm 2: Least Significant Bit
        └── RdhServiceImpl.java             # Dispatcher chọn thuật toán linh hoạt

🚀 Hướng dẫn khởi chạy (Getting Started)
1. Yêu cầu môi trường (Prerequisites)
Java OpenJDK 17 trở lên

Maven 3.9

Docker Desktop & Docker Compose (Tùy chọn)

Endpoint: POST /api/v1/rdh/embed

Query Params: algorithm (Tùy chọn: HISTOGRAM_SHIFTING hoặc LSB. Mặc định: HISTOGRAM_SHIFTING)

Content-Type: multipart/form-data

Body Form-Data:

image (File): File ảnh gốc (.png)

secretData (Text): Chuỗi văn bản cần giấu

Response: Tệp tin ảnh Stego stego_image.png (Binary File).

3. Trích xuất tin ẩn (Extract API)
Endpoint: POST /api/v1/rdh/extract

Query Params: algorithm (Tùy chọn, cần khớp với thuật toán lúc Embed)

Content-Type: multipart/form-data

Body Form-Data:

image (File): File ảnh Stego stego_image.png

Response (200 OK):
Đánh giá chất lượng ảnh (Evaluation API) 📈
Endpoint: POST /api/v1/rdh/evaluate

Content-Type: multipart/form-data

Body Form-Data:

originalImage (File): File ảnh gốc

stegoImage (File): File ảnh đã giấu tin

Response (200 OK):
{
  "code": 200,
  "message": "Thao tác thành công",
  "data": {
    "psnr": 52.14,
    "mse": 0.42,
    "maxCapacityBits": 262144,
    "maxCapacityBytes": 32768,
    "qualityAssessment": "Xuất sắc (Không thể nhận biết bằng mắt thường)"
  }
}
