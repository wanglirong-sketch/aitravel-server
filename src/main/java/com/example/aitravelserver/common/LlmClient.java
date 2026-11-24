package com.example.aitravelserver.common;

import com.example.aitravelserver.common.config.LlmProperties;
import com.example.aitravelserver.common.exception.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class LlmClient {

    private final LlmProperties props;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate rest = new RestTemplate();

    public LlmClient(LlmProperties props) {
        this.props = props;
    }

    /**
     * 单轮对话，返回模型输出的纯文本
     */

    /** 单轮对话，默认温度 0.2、最大输出 1500 token */
    public String chat(String system, String user) {
        return chat(system, user, 0.2, 1500);
    }

    /** 单轮对话，指定温度和最大输出长度 */
    public String chat(String system, String user, double temperature, int maxTokens) {
        // 1. 组装请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        Map<String, Object> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", system);

        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", user);

        body.put("messages", Arrays.asList(systemMsg, userMsg));

        // 2. 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(props.getApiKey());

        // 3. 发请求
        ResponseEntity<String> resp;
        try {
            resp = rest.postForEntity(props.getBaseUrl() + "/chat/completions",
                    new HttpEntity<>(body, headers), String.class);
        } catch (HttpStatusCodeException e) {
            throw new BizException(500, "模型调用失败 " + e.getStatusCode().value()
                    + ": " + e.getResponseBodyAsString());
        }

        // 4. 取 choices[0].message.content
        try {
            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isEmpty()) {
                throw new BizException(500, "模型未返回内容，原始响应: " + resp.getBody());
            }
            return content.asText();
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(500, "模型响应解析失败");
        }
    }


    //流式对话。每收到一段增量就回调 onDelta，方法返回拼接后的完整文本。
    public String chatStream(String system, String user, double temperature, int maxTokens,
                             Consumer<String> onDelta) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", props.getModel());
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", true);

        // 请求体中的系统消息和用户消息
        Map<String, Object> sys = new HashMap<>();
        sys.put("role", "system");
        sys.put("content", system);

        // 用户消息
        Map<String, Object> usr = new HashMap<>();
        usr.put("role", "user");
        usr.put("content", user);

        body.put("messages", Arrays.asList(sys, usr));

        StringBuilder full = new StringBuilder();
        // 发请求
        rest.execute(props.getBaseUrl() + "/chat/completions", HttpMethod.POST,
                request -> {
                    request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    request.getHeaders().setBearerAuth(props.getApiKey());
                    request.getHeaders().setAccept(
                            Arrays.asList(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));
                    try {
                        request.getBody().write(mapper.writeValueAsBytes(body));
                    } catch (IOException e) {
                        throw new RuntimeException("请求体写入失败", e);
                    }
                },
                response -> {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data:")) continue;
                            String data = line.substring(5).trim();
                            if ("[DONE]".equals(data)) break;
                            try {
                                JsonNode node = mapper.readTree(data);
                                String delta = node.path("choices").path(0)
                                        .path("delta").path("content").asText("");
                                if (!delta.isEmpty()) {
                                    full.append(delta);
                                    onDelta.accept(delta);
                                }
                            } catch (Exception ignored) {
                                // 单行解析失败不影响整体，跳过
                            }
                        }
                    }
                    return null;
                });

        return full.toString();
    }
}
