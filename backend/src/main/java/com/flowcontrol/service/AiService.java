package com.flowcontrol.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * AI 智能助手服务
 * 支持 OpenAI / 豆包 / DeepSeek / Ollama 等兼容 API 的 LLM 服务商
 */
@Slf4j
@Service
public class AiService {

    @Value("${ai.enabled:true}")
    private boolean aiEnabled;

    @Value("${ai.provider:openai}")
    private String provider;

    @Value("${ai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:gpt-3.5-turbo}")
    private String model;

    @Value("${ai.max-tokens:2048}")
    private int maxTokens;

    @Value("${ai.temperature:0.7}")
    private double temperature;

    @Value("${ai.timeout:60000}")
    private long timeout;

    @Value("${ai.max-history:20}")
    private int maxHistory;

    @Value("${ai.system-prompt:}")
    private String systemPrompt;

    @Autowired
    private com.flowcontrol.repository.TrafficPlanRepository trafficPlanRepository;

    @Autowired
    private com.flowcontrol.repository.MonitorRecordRepository monitorRecordRepository;

    @Autowired
    private com.flowcontrol.repository.NetworkStabilityRepository networkStabilityRepository;

    @Autowired
    private com.flowcontrol.repository.DeviceUsageRepository deviceUsageRepository;

    @Autowired
    private com.flowcontrol.repository.ServerPeakRepository serverPeakRepository;

    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    /** 会话ID -> 对话历史 */
    private final Map<String, List<ChatMessage>> sessionHistory = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeout))
                .followRedirect(true);

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("Content-Type", "application/json")
                .build();
        log.info("AI服务初始化完成，Provider: {}, Model: {}, Enabled: {}", provider, model, aiEnabled);
    }

    // ==================== 公开接口 ====================

    /**
     * 聊天接口
     * @param sessionId 会话ID（为空则创建新会话）
     * @param message 用户消息
     * @param includeContext 是否注入系统数据上下文
     * @return AI 回复
     */
    public String chat(String sessionId, String message, boolean includeContext) {
        if (!aiEnabled) {
            return "AI功能已禁用，请联系管理员启用。";
        }

        String sid = StrUtil.blankToDefault(sessionId, UUID.randomUUID().toString());
        List<ChatMessage> history = sessionHistory.computeIfAbsent(sid, k -> new ArrayList<>());

        // 构建消息列表
        List<ChatMessage> messages = buildMessages(history, message, includeContext);

        // 调用 AI
        String reply;
        try {
            reply = callAi(messages);
        } catch (Exception e) {
            log.error("AI调用失败: {}", e.getMessage(), e);
            return "AI服务暂时不可用，请检查API配置或稍后重试。错误信息：" + e.getMessage();
        }

        // 保存对话历史
        history.add(new ChatMessage("user", message));
        history.add(new ChatMessage("assistant", reply));
        // 限制历史长度
        if (history.size() > maxHistory * 2) {
            history.subList(0, history.size() - maxHistory * 2).clear();
        }

        return reply;
    }

    /**
     * 分析上传的 Excel 文件
     */
    public String analyzeFile(MultipartFile file) {
        if (!aiEnabled) {
            return "AI功能已禁用，请联系管理员启用。";
        }

        String content;
        try {
            content = extractExcelContent(file);
        } catch (Exception e) {
            log.error("Excel解析失败: {}", e.getMessage(), e);
            return "文件解析失败：" + e.getMessage();
        }

        if (content.length() > 8000) {
            content = content.substring(0, 8000) + "\n...（数据过长，已截断）";
        }

        String prompt = String.format(
                "请分析以下Excel数据内容，给出专业的数据解读、异常发现和优化建议：\n\n%s\n\n请用清晰的结构化格式回复，包括：1.数据概览 2.关键发现 3.建议措施",
                content
        );

        List<ChatMessage> messages = Arrays.asList(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", prompt)
        );

        try {
            return callAi(messages);
        } catch (Exception e) {
            log.error("AI文件分析失败: {}", e.getMessage(), e);
            return "AI分析失败：" + e.getMessage();
        }
    }

    /**
     * 获取支持的模型列表
     */
    public List<Map<String, String>> listModels() {
        List<Map<String, String>> models = new ArrayList<>();

        Map<String, String> gpt4 = new HashMap<>();
        gpt4.put("id", "gpt-4"); gpt4.put("name", "GPT-4"); gpt4.put("provider", "OpenAI");
        models.add(gpt4);

        Map<String, String> gpt35 = new HashMap<>();
        gpt35.put("id", "gpt-3.5-turbo"); gpt35.put("name", "GPT-3.5 Turbo"); gpt35.put("provider", "OpenAI");
        models.add(gpt35);

        Map<String, String> doubaoPro = new HashMap<>();
        doubaoPro.put("id", "doubao-pro-32k"); doubaoPro.put("name", "豆包 Pro 32K"); doubaoPro.put("provider", "Doubao");
        models.add(doubaoPro);

        Map<String, String> doubaoLite = new HashMap<>();
        doubaoLite.put("id", "doubao-lite-32k"); doubaoLite.put("name", "豆包 Lite 32K"); doubaoLite.put("provider", "Doubao");
        models.add(doubaoLite);

        Map<String, String> deepseekChat = new HashMap<>();
        deepseekChat.put("id", "deepseek-chat"); deepseekChat.put("name", "DeepSeek Chat"); deepseekChat.put("provider", "DeepSeek");
        models.add(deepseekChat);

        Map<String, String> deepseekCoder = new HashMap<>();
        deepseekCoder.put("id", "deepseek-coder"); deepseekCoder.put("name", "DeepSeek Coder"); deepseekCoder.put("provider", "DeepSeek");
        models.add(deepseekCoder);

        Map<String, String> qwenPlus = new HashMap<>();
        qwenPlus.put("id", "qwen-plus"); qwenPlus.put("name", "通义千问 Plus"); qwenPlus.put("provider", "Qwen");
        models.add(qwenPlus);

        return models;
    }

    /**
     * 获取/创建会话ID
     */
    public String createSession() {
        return UUID.randomUUID().toString();
    }

    /**
     * 清除会话历史
     */
    public void clearSession(String sessionId) {
        sessionHistory.remove(sessionId);
    }

    // ==================== 私有方法 ====================

    /**
     * 构建发送给 AI 的消息列表
     */
    private List<ChatMessage> buildMessages(List<ChatMessage> history, String newMessage, boolean includeContext) {
        List<ChatMessage> messages = new ArrayList<>();

        // 系统提示词
        String sysMsg = systemPrompt;
        if (includeContext) {
            sysMsg += "\n\n" + buildContextPrompt();
        }
        messages.add(new ChatMessage("system", sysMsg));

        // 历史记录（去掉最老的，保留最近N条）
        if (!history.isEmpty()) {
            int start = Math.max(0, history.size() - maxHistory * 2);
            messages.addAll(history.subList(start, history.size()));
        }

        // 新消息
        messages.add(new ChatMessage("user", newMessage));
        return messages;
    }

    /**
     * 构建系统数据上下文
     */
    private String buildContextPrompt() {
        StringBuilder ctx = new StringBuilder();
        ctx.append("\n\n===== 当前系统数据上下文 =====");

        try {
            // 流量规划统计
            long planCount = trafficPlanRepository.count();
            ctx.append(String.format("\n【流量规划】当前共有 %d 条规划记录。", planCount));

            // 监控记录（最近24小时）
            ctx.append(String.format("\n【实时监控】当前共有 %d 条监控记录。", monitorRecordRepository.count()));

            // 网络稳定性
            long healthyCount = networkStabilityRepository.countByStatus("HEALTHY");
            long warningCount = networkStabilityRepository.countByStatus("WARNING");
            long criticalCount = networkStabilityRepository.countByStatus("CRITICAL");
            ctx.append(String.format("\n【网络稳定性】HEALTHY: %d | WARNING: %d | CRITICAL: %d",
                    healthyCount, warningCount, criticalCount));

            // 设备使用
            long onlineCount = deviceUsageRepository.countByStatus("ONLINE");
            long offlineCount = deviceUsageRepository.countByStatus("OFFLINE");
            ctx.append(String.format("\n【设备使用】在线: %d | 离线: %d", onlineCount, offlineCount));

            // 服务器峰值
            long alertCount = serverPeakRepository.countByAlertLevelIsNotNull();
            ctx.append(String.format("\n【服务器峰值】当前共有 %d 条记录，其中 %d 条产生过告警。",
                    serverPeakRepository.count(), alertCount));

        } catch (Exception e) {
            ctx.append("\n（获取系统数据时出错：").append(e.getMessage()).append("）");
        }

        ctx.append("\n===== 上下文结束 =====");
        return ctx.toString();
    }

    /**
     * 调用 AI API（统一处理多服务商）
     */
    private String callAi(List<ChatMessage> messages) throws Exception {
        String endpoint;
        ObjectNode requestBody = objectMapper.createObjectNode();

        switch (provider.toLowerCase()) {
            case "doubao":
                endpoint = baseUrl + "/chat/completions";
                requestBody.put("model", model);
                requestBody.put("stream", false);
                requestBody.put("max_tokens", maxTokens);
                requestBody.put("temperature", temperature);
                break;
            case "deepseek":
                endpoint = baseUrl + "/chat/completions";
                requestBody.put("model", model);
                requestBody.put("stream", false);
                requestBody.put("max_tokens", maxTokens);
                requestBody.put("temperature", temperature);
                break;
            case "ollama":
                endpoint = baseUrl + "/api/chat";
                requestBody.put("model", model);
                requestBody.put("stream", false);
                break;
            default: // openai and others
                endpoint = baseUrl + "/chat/completions";
                requestBody.put("model", model);
                requestBody.put("stream", false);
                requestBody.put("max_tokens", maxTokens);
                requestBody.put("temperature", temperature);
                break;
        }

        // 构建消息数组
        ArrayNode msgArray = requestBody.putArray("messages");
        for (ChatMessage msg : messages) {
            ObjectNode msgNode = msgArray.addObject();
            msgNode.put("role", msg.role);
            msgNode.put("content", msg.content);
        }

        String response;
        try {
            response = webClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
        } catch (WebClientResponseException e) {
            throw new RuntimeException("API返回错误 " + e.getStatusCode() + ": " + e.getResponseBodyAsString());
        }

        if (response == null || response.isEmpty()) {
            throw new RuntimeException("AI返回空响应");
        }

        // 解析响应
        JsonNode root = objectMapper.readTree(response);
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("AI响应格式异常：" + response);
        }

        // Ollama 格式
        if ("ollama".equalsIgnoreCase(provider)) {
            return root.path("message").path("content").asText("");
        }

        return choices.get(0).path("message").path("content").asText("");
    }

    /**
     * 解析 Excel 文件内容
     */
    private String extractExcelContent(MultipartFile file) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("文件名：").append(file.getOriginalFilename()).append("\n");

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                sb.append("\n=== Sheet ").append(sheet.getSheetName()).append(" ===\n");

                // 表头
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    List<String> headers = new ArrayList<>();
                    for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                        Cell cell = headerRow.getCell(i);
                        headers.add(getCellValue(cell));
                    }
                    sb.append("表头：").append(String.join(" | ", headers)).append("\n");
                }

                // 数据行（最多100行）
                int rowCount = 0;
                for (Row row : sheet) {
                    if (row.getRowNum() == 0) continue; // 跳过表头
                    if (++rowCount > 100) {
                        sb.append("...（共").append(sheet.getLastRowNum()).append("行，已截断）\n");
                        break;
                    }
                    List<String> values = new ArrayList<>();
                    for (int i = 0; i < row.getLastCellNum(); i++) {
                        Cell cell = row.getCell(i);
                        values.add(getCellValue(cell));
                    }
                    sb.append(String.join(" | ", values)).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                return val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try { return String.valueOf(cell.getNumericCellValue()); }
                catch (Exception e) { return cell.getCellFormula(); }
            default: return "";
        }
    }

    // ==================== 内部类 ====================
    private static class ChatMessage {
        String role;
        String content;
        ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
