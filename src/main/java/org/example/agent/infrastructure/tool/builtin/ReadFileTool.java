package org.example.agent.infrastructure.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;
import org.example.agent.infrastructure.tool.ToolArguments;
import org.example.agent.infrastructure.tool.Workspace;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 读取工作区内的文本文件。
 */
@ApplicationScoped
public class ReadFileTool implements Tool {

    private static final int MAX_BYTES = 512 * 1024;

    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "path": {
                  "type": "string",
                  "description": "File path relative to the workspace root"
                }
              },
              "required": ["path"]
            }
            """;

    private final Workspace workspace;

    @Inject
    public ReadFileTool(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public String name() {
        return "read";
    }

    @Override
    public String description() {
        return "Read a UTF-8 text file in the workspace. Files larger than 512 KB are rejected.";
    }

    @Override
    public String inputSchema() {
        return INPUT_SCHEMA;
    }

    @Override
    public ToolResult execute(ToolCall call) {
        try {
            ToolArguments args = ToolArguments.parse(call.arguments());
            Path path = workspace.resolvePath(args.requireString("path"));

            if (!Files.exists(path)) {
                return ToolResult.error(call.callId(), "file not found: " + path);
            }
            if (Files.isDirectory(path)) {
                return ToolResult.error(call.callId(), "path is a directory: " + path);
            }

            long size = Files.size(path);
            if (size > MAX_BYTES) {
                return ToolResult.error(
                        call.callId(),
                        "file too large (" + size + " bytes), max " + MAX_BYTES);
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            return ToolResult.ok(call.callId(), content);
        } catch (Exception e) {
            return ToolResult.error(call.callId(), e.getMessage());
        }
    }
}
