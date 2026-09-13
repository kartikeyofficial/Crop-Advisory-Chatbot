package com.cropadvisory.controller;

import com.cropadvisory.dto.ChatRequest;
import com.cropadvisory.model.ChatMessage;
import com.cropadvisory.repository.ChatMessageRepository;
import com.cropadvisory.service.CropAdvisoryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/crop")
@CrossOrigin(origins = "http://localhost:4200")
public class CropController {

    private final CropAdvisoryService service;
    private final ChatMessageRepository repository;

    public CropController(CropAdvisoryService service, ChatMessageRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        return service.chatStream(request.getSessionId(), request.getMessage());
    }

    @GetMapping("/history/{sessionId}")
    public List<ChatMessage> history(@PathVariable String sessionId) {
        return repository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}
