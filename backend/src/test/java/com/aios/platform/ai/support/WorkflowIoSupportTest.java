package com.aios.platform.ai.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aios.platform.common.exception.BusinessException;
import com.aios.platform.runtime.dto.ChatExecutionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WorkflowIoSupportTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void validateInput_requiresQueryField() throws Exception {
        String schema =
                """
                {"type":"object","required":["query"],"properties":{"query":{"type":"string"}}}
                """;
        ObjectNode input = MAPPER.createObjectNode();
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> WorkflowIoSupport.validateInput("object", schema, input));
        assertTrue(ex.getMessage().contains("query"));
    }

    @Test
    void toPromptMessage_objectUsesQueryField() throws Exception {
        ObjectNode input = MAPPER.createObjectNode();
        input.put("query", "什么是 AIOS");
        assertEquals("什么是 AIOS", WorkflowIoSupport.toPromptMessage("object", input));
    }

    @Test
    void toPromptMessage_textPlainString() throws Exception {
        assertEquals("hello", WorkflowIoSupport.toPromptMessage("text", MAPPER.getNodeFactory().textNode("hello")));
    }

    @Test
    void formatOutput_textWrapsContent() {
        ChatExecutionResult exec = new ChatExecutionResult();
        exec.setReply("answer-text");
        @SuppressWarnings("unchecked")
        Map<String, String> out =
                (Map<String, String>) WorkflowIoSupport.formatOutput("text", null, exec);
        assertEquals("answer-text", out.get("content"));
    }

    @Test
    void formatOutput_messageRoleAssistant() {
        ChatExecutionResult exec = new ChatExecutionResult();
        exec.setReply("hi");
        @SuppressWarnings("unchecked")
        Map<String, String> out =
                (Map<String, String>) WorkflowIoSupport.formatOutput("message", null, exec);
        assertEquals("assistant", out.get("role"));
        assertEquals("hi", out.get("content"));
    }

    @Test
    void exampleInput_objectSchemaBuildsQueryDefault() throws Exception {
        String schema =
                """
                {"type":"object","properties":{"query":{"type":"string"}}}
                """;
        var ex = WorkflowIoSupport.exampleInput("object", schema);
        assertTrue(ex.has("query"));
    }
}
