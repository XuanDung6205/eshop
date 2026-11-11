package org.example.eshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiConfig {

    @Value("${spring.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${spring.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta/openai}")
    private String baseUrl;

    @Value("${spring.ai.gemini.model:gemini-3.5-flash-lite}")
    private String model;

    @Value("${spring.ai.gemini.temperature:0.7}")
    private Double temperature;

    /**
     * System Prompt chuẩn cho trợ lý bán hàng
     */
    public static final String DEFAULT_SYSTEM_PROMPT = """
            Bạn là trợ lý AI tư vấn thiết kế và trang trí nội thất thông minh, thân thiện của cửa hàng DecorHome.
            Nhiệm vụ của bạn:
            1. Tư vấn chi tiết, chính xác về các sản phẩm trang trí nội thất (đèn decor, tranh treo tường, bình hoa gốm sứ, gương, thảm trải sàn, gối tựa, đồng hồ, kệ gỗ, nến thơm, cây giả, tượng trưng bày, ghế thư giãn...).
            2. Gợi ý cách phối màu sắc, chất liệu và phong cách (Bắc Âu, Japandi, tối giản, Vintage) phù hợp với từng không gian (phòng khách, phòng ngủ, bàn làm việc, ban công).
            3. Hướng dẫn khách hàng các bước đặt hàng, kiểm tra giỏ hàng và thanh toán trên DecorHome.
            4. Giải đáp các chính sách của cửa hàng: đóng gói chống sốc 3 lớp cho đồ gốm/kính, giao hàng toàn quốc, bảo hành 1 đổi 1 trong 30 ngày nếu nứt vỡ khi vận chuyển.
            5. Luôn giữ thái độ lịch sự, tinh tế, súc tích bằng tiếng Việt.
            """;

    /**
     * Mẫu Prompt ngữ cảnh (RAG Template)
     */
    public static final String RAG_PROMPT_TEMPLATE = """
            Dưới đây là thông tin sản phẩm nội thất thực tế được truy xuất từ hệ thống DecorHome:
            ----------------------------------------
            %s
            ----------------------------------------
            Yêu cầu đối với trợ lý:
            - Ưu tiên sử dụng dữ liệu sản phẩm ở trên để tư vấn giá cả, chất liệu, kích thước, không gian phù hợp, tình trạng tồn kho và link xem chi tiết.
            - Nếu thông tin sản phẩm không có trong danh sách trên hoặc cửa hàng không kinh doanh, hãy thông báo lịch sự với khách hàng rằng sản phẩm hiện chưa có tại DecorHome, tuyệt đối không tự bịa đặt giá hoặc thông số sai sự thật.

            Hội thoại trước (chỉ dùng để hiểu các câu hỏi tiếp nối; dữ liệu sản phẩm ở trên mới là nguồn đáng tin cậy):
            %s

            Câu hỏi hiện tại của khách hàng: %s
            """;

    @Bean
    public RestClient geminiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(30));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public Double getTemperature() {
        return temperature;
    }
}
