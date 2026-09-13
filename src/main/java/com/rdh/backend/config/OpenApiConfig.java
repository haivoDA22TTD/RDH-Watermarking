package com.rdh.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Cấu hình OpenAPI/Swagger Documentation cho RDH Backend API
 * 
 * @author haivoDev
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(apiServers())
                .tags(apiTags());
    }

    /**
     * Thông tin metadata của API
     */
    private Info apiInfo() {
        return new Info()
                .title("🖼️ RDH Backend API - Reversible Data Hiding")
                .version("1.0.0")
                .description("""
                        ## Giới thiệu
                        
                        **RDH Backend API** cung cấp các dịch vụ xử lý ảnh với kỹ thuật **Reversible Data Hiding** (Giấu tin thuận nghịch).
                        
                        ### Tính năng chính:
                        
                        - **🔐 Embed**: Nhúng dữ liệu bí mật vào ảnh với thuật toán Histogram Shifting
                        - **🔓 Extract**: Giải mã và khôi phục dữ liệu từ ảnh stego
                        - **📊 Evaluate**: Đánh giá chất lượng ảnh stego (PSNR, SSIM, Capacity)
                        - **📦 Batch Processing**: Xử lý hàng loạt nhiều ảnh cùng lúc
                        
                        ### Thuật toán:
                        
                        **Histogram Shifting RDH** là thuật toán giấu tin thuận nghịch tiên tiến:
                        - ✅ Khôi phục 100% ảnh gốc sau khi extract
                        - ✅ PSNR > 50 dB (chất lượng rất cao)
                        - ✅ Không phát hiện được bằng mắt thường
                        - ✅ Capacity động phụ thuộc vào nội dung ảnh
                        
                        ### Hỗ trợ:
                        
                        - **Format ảnh**: PNG, JPG, BMP
                        - **Kích thước**: Không giới hạn (khuyến nghị < 10MB)
                        - **Batch size**: Tối đa 200MB/request
                        
                        ---
                        
                        📖 **Documentation**: Chi tiết tại `/docs` folder
                        
                        🔗 **GitHub**: [rdh-backend](https://github.com/haivoDA22TTD/rdh-backend)
                        """)
                .contact(new Contact()
                        .name("RDH Development Team")
                        .email("support@rdh-backend.com")
                        .url("https://rdh-backend.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));
    }

    /**
     * Danh sách servers
     */
    private List<Server> apiServers() {
        Server localServer = new Server()
                .url("http://localhost:8080")
                .description("🏠 Local Development Server");

        Server productionServer = new Server()
                .url("https://api.rdh-backend.com")
                .description("🌐 Production Server");

        return List.of(localServer, productionServer);
    }

    /**
     * Tags để phân nhóm endpoints
     */
    private List<Tag> apiTags() {
        return List.of(
                new Tag()
                        .name("📋 System")
                        .description("Thông tin hệ thống và cấu hình"),
                new Tag()
                        .name("🔐 Single Processing")
                        .description("Xử lý từng ảnh riêng lẻ (Embed, Extract, Evaluate)"),
                new Tag()
                        .name("📦 Batch Processing")
                        .description("Xử lý hàng loạt nhiều ảnh cùng lúc")
        );
    }
}
