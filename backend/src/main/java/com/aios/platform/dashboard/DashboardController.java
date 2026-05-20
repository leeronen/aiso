package com.aios.platform.dashboard;

import com.aios.platform.ai.mapper.AiAgentMapper;
import com.aios.platform.ai.mapper.AiModelMapper;
import com.aios.platform.chat.mapper.ChatMessageMapper;
import com.aios.platform.chat.mapper.ChatSessionMapper;
import com.aios.platform.common.api.ApiResponse;
import com.aios.platform.kb.mapper.KbDocumentMapper;
import com.aios.platform.kb.mapper.KbKnowledgeBaseMapper;
import com.aios.platform.system.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final SysUserMapper userMapper;
    private final AiModelMapper modelMapper;
    private final AiAgentMapper agentMapper;
    private final KbKnowledgeBaseMapper kbMapper;
    private final KbDocumentMapper docMapper;
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public ApiResponse<Map<String, Object>> summary() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userTotal", userMapper.selectCount(new LambdaQueryWrapper<>()));
        m.put("modelTotal", modelMapper.selectCount(new LambdaQueryWrapper<>()));
        m.put("agentTotal", agentMapper.selectCount(new LambdaQueryWrapper<>()));
        m.put("knowledgeBaseTotal", kbMapper.selectCount(new LambdaQueryWrapper<>()));
        m.put("documentTotal", docMapper.selectCount(new LambdaQueryWrapper<>()));
        m.put("sessionTotal", sessionMapper.selectCount(new LambdaQueryWrapper<>()));
        m.put("messageTotal", messageMapper.selectCount(new LambdaQueryWrapper<>()));
        m.put("onlineUsers", 0);
        m.put("note", "在线用户等指标可接入 Redis / 网关后替换");
        return ApiResponse.ok(m);
    }
}
