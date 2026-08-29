package org.example.agent.infrastructure.sandbox;

import org.example.agent.domain.workspace.Workspace;

import java.nio.file.Path;

/**
 * 在工作区范围内选择命令执行的工作目录。
 */
public interface WorkingDirectorySelector {

    /**
     * @param workspace 本轮工作区
     * @param requested 工具请求的相对/绝对路径；{@code null} 或空白表示使用工作区根
     * @return 规范化后、且落在工作区内的目录路径
     */
    Path select(Workspace workspace, String requested);
}
