package com.cropadvisory.service;

import com.cropadvisory.model.ChatMessage;
import com.cropadvisory.repository.ChatMessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CropAdvisoryService {

    private final ChatMessageRepository repository;
    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ai.api.model}")
    private String model;

    private static final String SYSTEM_PROMPT = """
        You are an expert agricultural advisor with deep knowledge of crop
        science, pest and disease management, soil health, irrigation practices,
        and sustainable farming. Your guidance should align with SDG 15 (Life on
        Land) and SDG 2 (Zero Hunger) — favoring practices that protect soil and
        ecosystem health over purely short-term yield.

        Your role: help farmers and students get practical, actionable crop
        advice through natural conversation.

        When a user describes their crop situation, try to identify:
        - Crop name
        - Growth stage (if mentioned)
        - Symptom or concern (e.g. yellowing leaves, pest sighting, low yield)
        - Region or season (if mentioned)

        If a detail critical to giving safe advice is missing, ask ONE
        clarifying question before advising — don't guess at an uncertain pest
        or disease identification.

        Structure each piece of advice as a bulleted list using '- ' for each point:
        - Likely cause: 1-2 sentences, plain language
        - Recommended action: specific, doable steps
        - Preventive tip for next season
        - Sustainability note: a brief note where relevant

        Rules:
        1. Never recommend a specific commercial pesticide or fertilizer brand
           — describe the category instead (e.g. "a neem-based organic
           pesticide," not a brand name).
        2. If the described symptom could indicate a serious, fast-spreading
           disease, advise consulting a local agricultural extension officer
           immediately, in addition to your suggestion.
        3. Keep each response conversational — a few sentences per point, not
           an essay.
        4. If the description is too vague to be confident, say so honestly and
           ask for more detail rather than guessing.
        5. Mention water or pesticide conservation where relevant to the advice.
        6. ALWAYS use bullet points ('- ') for listing items or steps instead of numbers.
        """;

    public CropAdvisoryService(ChatMessageRepository repository, @Value("${ai.api.url}") String apiUrl, @Value("${ai.api.key}") String apiKey) {
        this.repository = repository;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    public Flux<String> chatStream(String sessionId, String userMessage) {
        // 1. Load prior turns for this conversation
        List<ChatMessage> history = repository.findBySessionIdOrderByTimestampAsc(sessionId);

        // 2. Build the full message list: system prompt + history + new message
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        for (ChatMessage m : history) {
            messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        // 3. Save the user's message now
        repository.save(new ChatMessage(sessionId, "user", userMessage));

        // 4. Prepare payload for streaming
        Map<String, Object> body = Map.of(
            "model", model,
            "messages", messages,
            "temperature", 0.4,
            "stream", true
        );

        // 5. Stream and accumulate reply
        StringBuilder fullReply = new StringBuilder();

        return webClient.post()
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> !line.trim().equals("[DONE]"))
                .map(this::extractDeltaContentFromString)
                .filter(content -> !content.isEmpty())
                .doOnNext(fullReply::append)
                .doOnComplete(() -> {
                    // 6. Save the fully accumulated reply to database
                    repository.save(new ChatMessage(sessionId, "assistant", fullReply.toString()));
                });
    }

    private String extractDeltaContentFromString(String json) {
        try {
            var root = mapper.readTree(json);
            var choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                var content = choices.get(0).path("delta").path("content");
                if (!content.isMissingNode() && !content.isNull()) {
                    return content.asText();
                }
            }
        } catch (Exception e) {
            // Ignore parse errors on empty or non-JSON chunks
        }
        return "";
    }
}
