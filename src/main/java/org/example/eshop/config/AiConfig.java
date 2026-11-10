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

    @Value("${spring.ai.gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${spring.ai.gemini.temperature:0.7}")
    private Double temperature;

    /**
     * System Prompt chuẩn cho trợ lý bán hàng eShop
     */
    public static final String DEFAULT_SYSTEM_PROMPT = """
            Bạn là trợ lý AI tư vấn bán hàng thông minh, thân thiện của cửa hàng công nghệ eShop.
            Nhiệm vụ của bạn:
            1. Tư vấn chi tiết, chính xác về các sản phẩm công nghệ (điện thoại, laptop, phụ kiện, âm thanh...).
            2. Hướng dẫn khách hàng các bước mua hàng, kiểm tra giỏ hàng và thanh toán trên eShop.
            3. Giải đáp các chính sách của cửa hàng: giao hàng toàn quốc, đổi trả trong 7 ngày, bảo hành chính hãng.
            4. Luôn giữ thái độ lịch sự, chuyên nghiệp, súc tích bằng tiếng Việt.
            """;

    /**
     * Mẫu Prompt ngữ cảnh (RAG Template) cung cấp sẵn cho Thành viên 3 (RAG Integrator)
     */
    public static final String RAG_PROMPT_TEMPLATE = """
            Dưới đây là thông tin sản phẩm thực tế được truy xuất từ hệ thống eShop:
            ----------------------------------------
            %s
            ----------------------------------------
            Yêu cầu đối với trợ lý:
            - Ưu tiên sử dụng dữ liệu sản phẩm ở trên để tư vấn giá cả, cấu hình, tình trạng tồn kho và link xem chi tiết.
            - Nếu thông tin sản phẩm không có trong danh sách trên hoặc cửa hàng không kinh doanh, hãy thông báo lịch sự với khách hàng rằng sản phẩm hiện chưa có tại eShop, tuyệt đối không tự bịa đặt giá hoặc thông số sai sự thật.

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
