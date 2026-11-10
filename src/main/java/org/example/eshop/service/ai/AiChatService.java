package org.example.eshop.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.eshop.config.AiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final RestClient geminiRestClient;
    private final AiConfig aiConfig;

    // Cache model khả dụng đã tìm thấy để không cần query ListModels mỗi lần
    private volatile String discoveredModel = null;
    private volatile String discoveredApiVersion = "v1beta";

    public AiChatService(RestClient geminiRestClient, AiConfig aiConfig) {
        this.geminiRestClient = geminiRestClient;
        this.aiConfig = aiConfig;
    }

    /**
     * Chat thông thường với Gemini AI (hỏi đáp chính sách, hướng dẫn sử dụng eShop)
     */
    public String chat(String userMessage) {
        return chat(userMessage, null);
    }

    /**
     * Chat có ngữ cảnh (RAG integration point dành riêng cho Thành viên 3 cắm Vector DB vào)
     */
    public String chatWithContext(String userMessage, String contextData) {
        return chatWithContext(userMessage, contextData, "");
    }

    public String chatWithContext(String userMessage, String contextData, String conversationHistory) {
        if (!StringUtils.hasText(contextData)) {
            return chat(userMessage);
        }
        String history = StringUtils.hasText(conversationHistory)
                ? conversationHistory.trim()
                : "Không có hội thoại trước.";
        String promptWithContext = String.format(
                AiConfig.RAG_PROMPT_TEMPLATE, contextData.trim(), history, userMessage.trim());
        return chatInternal(promptWithContext);
    }

    /**
     * Phương thức chat tổng quát nhận câu hỏi và ngữ cảnh tuỳ chọn
     */
    public String chat(String userMessage, String contextData) {
        if (StringUtils.hasText(contextData)) {
            return chatWithContext(userMessage, contextData);
        }
        return chatInternal(userMessage);
    }

    private String chatInternal(String userPrompt) {
        String apiKey = aiConfig.getApiKey();

        if (!StringUtils.hasText(apiKey)) {
            log.warn("Gemini API Key chưa được cấu hình trong biến môi trường GEMINI_API_KEY. Trả về thông báo demo.");
            return "[eShop AI Demo]: Bạn chưa cấu hình Google Gemini API Key hợp lệ. " +
                    "Vui lòng cấu hình biến môi trường GEMINI_API_KEY.";
        }

        apiKey = apiKey.trim();

        // 1. Thử gọi với model đã discover trước đó (nếu có) hoặc model trong config
        String modelToUse = StringUtils.hasText(discoveredModel) ? discoveredModel : aiConfig.getModel();
        String apiVer = discoveredApiVersion;

        try {
            return executeNativeCall(userPrompt, apiKey, modelToUse, apiVer);
        } catch (Exception ex) {
            log.warn("Gọi model {} ({}) gặp lỗi: {}. Đang tự động dò tìm model khả dụng từ Google AI Studio...",
                    modelToUse, apiVer, ex.getMessage());

            // 2. Tự động truy vấn ModelService.ListModels để tìm model chính xác mà API Key này được phép dùng
            try {
                DiscoveredInfo info = discoverSupportedModel(apiKey);
                if (info != null && StringUtils.hasText(info.modelName)) {
                    this.discoveredModel = info.modelName;
                    this.discoveredApiVersion = info.apiVersion;
                    log.info("Đã tìm thấy model tương thích: {} ({})", info.modelName, info.apiVersion);
                    return executeNativeCall(userPrompt, apiKey, info.modelName, info.apiVersion);
                }
            } catch (Exception discoverEx) {
                log.warn("Không thể tự động ListModels: {}", discoverEx.getMessage());
            }

            // 3. Fallback sang danh sách các tên model phổ biến khác trên cả v1 và v1beta
            String[] fallbackModels = {"gemini-2.0-flash", "gemini-1.5-flash-latest", "gemini-1.5-flash-001", "gemini-1.5-flash-002", "gemini-1.5-pro"};
            String[] versions = {"v1beta", "v1"};

            for (String ver : versions) {
                for (String m : fallbackModels) {
                    try {
                        String result = executeNativeCall(userPrompt, apiKey, m, ver);
                        this.discoveredModel = m;
                        this.discoveredApiVersion = ver;
                        log.info("Fallback thành công với model {} ({})", m, ver);
                        return result;
                    } catch (Exception ignored) {}
                }
            }

            // 4. Nếu tất cả đều không được, trả về lỗi chi tiết để người dùng kiểm tra
            return "Đã xảy ra sự cố khi kết nối tới dịch vụ AI của Google Gemini: " + ex.getMessage() +
                    ". Vui lòng kiểm tra lại API Key hoặc quyền truy cập của tài khoản trên Google AI Studio.";
        }
    }

    /**
     * Thực hiện gửi request generateContent tới Google Gemini Native REST API
     */
    private String executeNativeCall(String userPrompt, String apiKey, String model, String apiVersion) {
        String cleanModel = model.startsWith("models/") ? model.substring(7) : model;
        String nativeUri = String.format(
                "https://generativelanguage.googleapis.com/%s/models/%s:generateContent?key=%s",
                apiVersion, cleanModel, apiKey
        );

        GeminiNativeRequest payload = new GeminiNativeRequest(
                AiConfig.DEFAULT_SYSTEM_PROMPT,
                userPrompt,
                aiConfig.getTemperature()
        );

        GeminiNativeResponse response = geminiRestClient.post()
                .uri(nativeUri)
                .header("x-goog-api-key", apiKey)
                .body(payload)
                .retrieve()
                .body(GeminiNativeResponse.class);

        if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            GeminiCandidate candidate = response.getCandidates().get(0);
            if (candidate.getFinishReason() != null && !"STOP".equalsIgnoreCase(candidate.getFinishReason())) {
                log.warn("Gemini phản hồi dừng bất thường [finishReason: {}]", candidate.getFinishReason());
            }
            if (candidate.getContent() != null && candidate.getContent().getParts() != null && !candidate.getContent().getParts().isEmpty()) {
                StringBuilder replyBuilder = new StringBuilder();
                for (GeminiPart part : candidate.getContent().getParts()) {
                    if (part != null && !Boolean.TRUE.equals(part.getThought()) && StringUtils.hasText(part.getText())) {
                        replyBuilder.append(part.getText());
                    }
                }
                String reply = replyBuilder.toString().trim();
                if (StringUtils.hasText(reply)) {
                    return reply;
                }
            }
        }

        return "Xin lỗi bạn, trợ lý eShop chưa thể đưa ra câu trả lời lúc này. Bạn vui lòng thử lại sau giây lát nhé!";
    }

    /**
     * Tự động truy vấn ModelService.ListModels từ Google để biết chính xác API Key này có quyền với model nào
     */
    private DiscoveredInfo discoverSupportedModel(String apiKey) {
        String[] versions = {"v1beta", "v1"};
        for (String ver : versions) {
            try {
                String listUrl = String.format("https://generativelanguage.googleapis.com/%s/models?key=%s", ver, apiKey);
                GeminiModelListResponse listResp = geminiRestClient.get()
                        .uri(listUrl)
                        .retrieve()
                        .body(GeminiModelListResponse.class);

                if (listResp != null && listResp.getModels() != null && !listResp.getModels().isEmpty()) {
                    // Ưu tiên model chứa flash
                    for (GeminiModelItem item : listResp.getModels()) {
                        if (item.supportsGenerateContent() && item.getName() != null && item.getName().contains("flash")) {
                            return new DiscoveredInfo(item.getName(), ver);
                        }
                    }
                    // Nếu không có flash, lấy model bất kỳ hỗ trợ generateContent
                    for (GeminiModelItem item : listResp.getModels()) {
                        if (item.supportsGenerateContent() && item.getName() != null) {
                            return new DiscoveredInfo(item.getName(), ver);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Lỗi khi kiểm tra ListModels ở {}: {}", ver, e.getMessage());
            }
        }
        return null;
    }

    private static class DiscoveredInfo {
        final String modelName;
        final String apiVersion;
        DiscoveredInfo(String modelName, String apiVersion) {
            this.modelName = modelName;
            this.apiVersion = apiVersion;
        }
    }

    // ================= DTOs Java thuần dành cho Native Google Gemini REST API =================

    public static class GeminiNativeRequest {
        @JsonProperty("system_instruction")
        private GeminiContent systemInstruction;
        private List<GeminiContent> contents;
        private GeminiGenerationConfig generationConfig;

        public GeminiNativeRequest() {}

        public GeminiNativeRequest(String systemPrompt, String userPrompt, Double temperature) {
            this.systemInstruction = new GeminiContent(List.of(new GeminiPart(systemPrompt)));
            this.contents = List.of(new GeminiContent("user", List.of(new GeminiPart(userPrompt))));
            if (temperature != null) {
                this.generationConfig = new GeminiGenerationConfig(temperature);
            }
        }

        public GeminiContent getSystemInstruction() { return systemInstruction; }
        public void setSystemInstruction(GeminiContent systemInstruction) { this.systemInstruction = systemInstruction; }
        public List<GeminiContent> getContents() { return contents; }
        public void setContents(List<GeminiContent> contents) { this.contents = contents; }
        public GeminiGenerationConfig getGenerationConfig() { return generationConfig; }
        public void setGenerationConfig(GeminiGenerationConfig generationConfig) { this.generationConfig = generationConfig; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiContent {
        private String role;
        private List<GeminiPart> parts;

        public GeminiContent() {}

        public GeminiContent(List<GeminiPart> parts) {
            this.parts = parts;
        }

        public GeminiContent(String role, List<GeminiPart> parts) {
            this.role = role;
            this.parts = parts;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public List<GeminiPart> getParts() { return parts; }
        public void setParts(List<GeminiPart> parts) { this.parts = parts; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiPart {
        private String text;
        private Boolean thought;

        public GeminiPart() {}

        public GeminiPart(String text) {
            this.text = text;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public Boolean getThought() { return thought; }
        public void setThought(Boolean thought) { this.thought = thought; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GeminiGenerationConfig {
        private Double temperature;
        private Integer maxOutputTokens = 2048;

        public GeminiGenerationConfig() {}

        public GeminiGenerationConfig(Double temperature) {
            this.temperature = temperature;
        }

        public Double getTemperature() { return temperature; }
        public void setTemperature(Double temperature) { this.temperature = temperature; }
        public Integer getMaxOutputTokens() { return maxOutputTokens; }
        public void setMaxOutputTokens(Integer maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiNativeResponse {
        private List<GeminiCandidate> candidates;

        public GeminiNativeResponse() {}

        public List<GeminiCandidate> getCandidates() { return candidates; }
        public void setCandidates(List<GeminiCandidate> candidates) { this.candidates = candidates; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiCandidate {
        private GeminiContent content;
        @JsonProperty("finishReason")
        private String finishReason;

        public GeminiCandidate() {}

        public GeminiContent getContent() { return content; }
        public void setContent(GeminiContent content) { this.content = content; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiModelListResponse {
        private List<GeminiModelItem> models;

        public GeminiModelListResponse() {}

        public List<GeminiModelItem> getModels() { return models; }
        public void setModels(List<GeminiModelItem> models) { this.models = models; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GeminiModelItem {
        private String name;
        private List<String> supportedGenerationMethods;

        public GeminiModelItem() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<String> getSupportedGenerationMethods() { return supportedGenerationMethods; }
        public void setSupportedGenerationMethods(List<String> supportedGenerationMethods) {
            this.supportedGenerationMethods = supportedGenerationMethods;
        }

        public boolean supportsGenerateContent() {
            return supportedGenerationMethods != null && supportedGenerationMethods.contains("generateContent");
        }
    }
}
