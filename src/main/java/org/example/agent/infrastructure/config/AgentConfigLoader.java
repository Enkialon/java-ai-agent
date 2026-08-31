package org.example.agent.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 加载 {@code agent.yaml}：外部路径优先，否则 classpath。
 * <p>
 * 查找顺序：
 * <ol>
 *   <li>系统属性 {@code agent.config}</li>
 *   <li>环境变量 {@code AGENT_CONFIG}</li>
 *   <li>工作目录 {@code ./agent.yaml}（适合 jar 旁挂配置）</li>
 *   <li>classpath {@code /agent.yaml}</li>
 * </ol>
 * 字符串中的 {@code ${ENV}} / {@code ${ENV:default}} 会在加载后展开。
 */
public final class AgentConfigLoader {

    private static final Pattern ENV_PATTERN =
            Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)(?::([^}]*))?}");

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

    private AgentConfigLoader() {
    }

    public static AgentConfig load() {
        String override = firstNonBlank(
                System.getProperty("agent.config"),
                System.getenv("AGENT_CONFIG"));
        if (override != null) {
            return load(Path.of(override));
        }

        Path local = Path.of("agent.yaml");
        if (Files.isRegularFile(local)) {
            return load(local);
        }

        try (InputStream in = AgentConfigLoader.class.getResourceAsStream("/agent.yaml")) {
            if (in == null) {
                return AgentConfig.defaults();
            }
            return parse(in);
        } catch (IOException e) {
            throw new IllegalStateException("failed to load classpath agent.yaml", e);
        }
    }

    public static AgentConfig load(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        try (Reader reader = Files.newBufferedReader(path)) {
            return parse(reader);
        } catch (IOException e) {
            throw new IllegalStateException("failed to load agent config: " + path, e);
        }
    }

    public static AgentConfig parse(InputStream in) throws IOException {
        return fromRoot(YAML.readValue(in, AgentConfigRoot.class));
    }

    public static AgentConfig parse(Reader reader) throws IOException {
        return fromRoot(YAML.readValue(reader, AgentConfigRoot.class));
    }

    public static AgentConfig parseYaml(String yaml) {
        try {
            return fromRoot(YAML.readValue(yaml, AgentConfigRoot.class));
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid agent yaml", e);
        }
    }

    private static AgentConfig fromRoot(AgentConfigRoot root) {
        AgentConfig config = root == null || root.agent() == null
                ? AgentConfig.defaults()
                : root.agent();
        return expand(config);
    }

    private static AgentConfig expand(AgentConfig config) {
        Map<String, AgentConfig.ModelClientSettings> clients = config.model().clients().entrySet()
                .stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> expandClient(e.getValue())));
        return new AgentConfig(
                new AgentConfig.ModelConfig(config.model().active(), clients),
                config.permissions(),
                config.loop());
    }

    private static AgentConfig.ModelClientSettings expandClient(
            AgentConfig.ModelClientSettings settings) {
        return new AgentConfig.ModelClientSettings(
                settings.type(),
                expandString(settings.apiKey()),
                expandString(settings.baseUrl()),
                expandString(settings.model()));
    }

    static String expandString(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        Matcher matcher = ENV_PATTERN.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String envName = matcher.group(1);
            String defaultValue = matcher.group(2);
            String envValue = System.getenv(envName);
            String replacement = (envValue != null && !envValue.isBlank())
                    ? envValue
                    : (defaultValue != null ? defaultValue : "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        String expanded = sb.toString().trim();
        return expanded.isEmpty() ? null : expanded;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        if (b != null && !b.isBlank()) {
            return b.trim();
        }
        return null;
    }

    /**
     * YAML 根：{@code agent:} 节点。
     */
    private record AgentConfigRoot(AgentConfig agent) {
    }
}
