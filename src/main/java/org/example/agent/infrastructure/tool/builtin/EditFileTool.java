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
 * 用精确字符串替换局部修改已有文件。
 */
@ApplicationScoped
public class EditFileTool implements Tool {

    private static final String INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "path": {
                  "type": "string",
                  "description": "File path relative to the workspace root"
                },
                "old_string": {
                  "type": "string",
                  "description": "Exact text to find in the file (must be unique)"
                },
                "new_string": {
                  "type": "string",
                  "description": "Replacement text"
                }
              },
              "required": ["path", "old_string", "new_string"]
            }
            """;

    @Inject
    public EditFileTool() {
    }

    @Override
    public String name() {
        return "edit";
    }

    @Override
    public String description() {
        return "Edit an existing file by replacing one unique occurrence of old_string with new_string.";
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
            String oldString = args.requireString("old_string");
            String newString = args.requireString("new_string");

            if (!Files.exists(path)) {
                return ToolResult.error(call.callId(), "file not found: " + path);
            }
            if (Files.isDirectory(path)) {
                return ToolResult.error(call.callId(), "path is a directory: " + path);
            }
            if (oldString.isEmpty()) {
                return ToolResult.error(call.callId(), "old_string must not be empty");
            }
            if (oldString.equals(newString)) {
                return ToolResult.error(call.callId(), "old_string and new_string are identical");
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            int first = content.indexOf(oldString);
            if (first < 0) {
                return ToolResult.error(call.callId(), "old_string not found in file");
            }
            int second = content.indexOf(oldString, first + oldString.length());
            if (second >= 0) {
                return ToolResult.error(
                        call.callId(),
                        "old_string is not unique in file; provide more context");
            }

            String updated = content.substring(0, first)
                    + newString
                    + content.substring(first + oldString.length());
            Files.writeString(path, updated, StandardCharsets.UTF_8);
            return ToolResult.ok(
                    call.callId(),
                    "edited " + workspace.root().relativize(path));
        } catch (Exception e) {
            return ToolResult.error(call.callId(), e.getMessage());
        }
    }
}
