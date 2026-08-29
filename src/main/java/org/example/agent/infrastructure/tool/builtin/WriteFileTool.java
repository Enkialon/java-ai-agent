package org.example.agent.infrastructure.tool.builtin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.example.agent.domain.tool.Tool;
import org.example.agent.domain.tool.ToolCall;
import org.example.agent.domain.tool.ToolResult;
import org.example.agent.domain.workspace.Workspace;
import org.example.agent.infrastructure.tool.ToolArguments;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 新建或完整覆盖写入工作区内的文本文件。
 */
@ApplicationScoped
public class WriteFileTool implements Tool {

    private static final int MAX_CONTENT_CHARS = 1_000_000;

    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "path": {
                  "type": "string",
                  "description": "File path relative to the workspace root"
                },
                "content": {
                  "type": "string",
                  "description": "Full file content to write"
                }
              },
              "required": ["path", "content"]
            }
            """;

    @Inject
    public WriteFileTool() {
    }

    @Override
    public String name() {
        return "write";
    }

    @Override
    public String description() {
        return "Create a new file or overwrite an existing file with the given content "
                + "(max " + MAX_CONTENT_CHARS + " chars).";
    }

    @Override
    public String inputSchema() {
        return INPUT_SCHEMA;
    }

    @Override
    public ToolResult execute(ToolCall call, Workspace workspace) {
        try {
            ToolArguments args = ToolArguments.parse(call.arguments());
            Path path = workspace.resolvePath(args.requireString("path"));
            String content = args.requireString("content");

            if (content.length() > MAX_CONTENT_CHARS) {
                return ToolResult.error(
                        call.callId(),
                        "content too large (" + content.length() + " chars), max " + MAX_CONTENT_CHARS);
            }
            if (Files.isDirectory(path)) {
                return ToolResult.error(call.callId(), "path is a directory: " + path);
            }

            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            boolean created = !Files.exists(path);
            Files.writeString(path, content, StandardCharsets.UTF_8);
            String action = created ? "created" : "wrote";
            return ToolResult.ok(
                    call.callId(),
                    action + " " + workspace.root().relativize(path)
                            + " (" + content.length() + " chars)");
        } catch (Exception e) {
            return ToolResult.error(call.callId(), e.getMessage());
        }
    }
}
