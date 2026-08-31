package org.example.agent.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 根配置：{@code agent.yaml} 反序列化结果。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AgentConfig(ModelConfig model, PermissionsConfig permissions, LoopConfig loop) {

    public AgentConfig {
        model = Objects.requireNonNullElseGet(model, ModelConfig::defaults);
        permissions = Objects.requireNonNullElseGet(permissions, PermissionsConfig::defaults);
        loop = Objects.requireNonNullElseGet(loop, LoopConfig::defaults);
    }

    public static AgentConfig defaults() {
        return new AgentConfig(ModelConfig.defaults(), PermissionsConfig.defaults(), LoopConfig.defaults());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelConfig(String active, Map<String, ModelClientSettings> clients) {

        public ModelConfig {
            active = (active == null || active.isBlank()) ? "stub" : active.trim();
            clients = clients == null ? Map.of() : Map.copyOf(clients);
        }

        public static ModelConfig defaults() {
            return new ModelConfig("stub", Map.of(
                    "stub", ModelClientSettings.stub()));
        }

        public ModelClientSettings activeClient() {
            ModelClientSettings settings = clients.get(active);
            if (settings == null) {
                throw new IllegalStateException(
                        "agent.model.active='" + active + "' not found in agent.model.clients");
            }
            return settings;
        }
    }

    /**
     * 单个 ModelClient 实例配置。{@code type} 决定实现：stub / deepseek / openai。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelClientSettings(
            String type,
            String apiKey,
            String baseUrl,
            String model
    ) {

        public ModelClientSettings {
            type = Objects.requireNonNull(type, "type must not be null").trim().toLowerCase();
            apiKey = blankToNull(apiKey);
            baseUrl = blankToNull(baseUrl);
            model = blankToNull(model);
        }

        public static ModelClientSettings stub() {
            return new ModelClientSettings("stub", null, null, null);
        }

        public Optional<String> apiKeyOptional() {
            return Optional.ofNullable(apiKey);
        }

        public Optional<String> baseUrlOptional() {
            return Optional.ofNullable(baseUrl);
        }

        public Optional<String> modelOptional() {
            return Optional.ofNullable(model);
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PermissionsConfig(
            String bash,
            String write,
            String network
    ) {

        public PermissionsConfig {
            bash = Objects.requireNonNullElse(bash, "allow");
            write = Objects.requireNonNullElse(write, "allow");
            network = Objects.requireNonNullElse(network, "deny");
        }

        public static PermissionsConfig defaults() {
            return new PermissionsConfig("allow", "allow", "deny");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoopConfig(Integer maxTurns) {

        private static final int DEFAULT_MAX_TURNS = 20;

        public LoopConfig {
            if (maxTurns == null || maxTurns < 1) {
                maxTurns = DEFAULT_MAX_TURNS;
            }
        }

        public static LoopConfig defaults() {
            return new LoopConfig(DEFAULT_MAX_TURNS);
        }
    }
}
