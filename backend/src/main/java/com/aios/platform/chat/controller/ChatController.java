package com.aios.platform.chat.controller;

import com.aios.platform.chat.dto.ChatSessionVO;
import com.aios.platform.chat.dto.ChatTokenStatsVO;
import com.aios.platform.chat.entity.ChatMessage;
import com.aios.platform.chat.service.ChatService;
import com.aios.platform.common.api.ApiResponse;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/sessions")
    @PreAuthorize("hasAuthority('chat:session:view')")
    public ApiResponse<Page<ChatSessionVO>> sessions(
            @RequestParam(defaultValue = "1") long current, @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(chatService.sessions(current, size));
    }

    public record CreateSessionBody(Long agentId, Long workflowId, String title) {}

    @PostMapping("/sessions")
    @PreAuthorize("hasAnyAuthority('chat:session:add','chat:session:update')")
    public ApiResponse<Map<String, Long>> createSession(@RequestBody CreateSessionBody body) {
        Long id = chatService.createSession(body.agentId(), body.workflowId(), body.title());
        return ApiResponse.ok(Map.of("sessionId", id));
    }

    public record UpdateSessionBody(Long agentId, Long workflowId, String title) {}

    @PutMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('chat:session:update')")
    public ApiResponse<Void> updateSession(
            @PathVariable Long sessionId, @RequestBody UpdateSessionBody body) {
        chatService.updateSession(sessionId, body.agentId(), body.workflowId(), body.title());
        return ApiResponse.ok();
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("hasAuthority('chat:session:delete')")
    public ApiResponse<Void> deleteSession(@PathVariable Long sessionId) {
        chatService.deleteSession(sessionId);
        return ApiResponse.ok();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasAuthority('chat:session:view')")
    public ApiResponse<List<ChatMessage>> messages(@PathVariable Long sessionId) {
        return ApiResponse.ok(chatService.messages(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/token-stats")
    @PreAuthorize("hasAuthority('chat:session:view')")
    public ApiResponse<ChatTokenStatsVO> tokenStats(@PathVariable Long sessionId) {
        return ApiResponse.ok(chatService.tokenStats(sessionId));
    }

    public record SendBody(String content) {}

    @PostMapping("/sessions/{sessionId}/messages")
    @PreAuthorize("hasAnyAuthority('chat:session:add','chat:session:update')")
    public ApiResponse<List<ChatMessage>> send(@PathVariable Long sessionId, @RequestBody SendBody body) {
        return ApiResponse.ok(chatService.sendUserMessage(sessionId, body.content()));
    }
}
