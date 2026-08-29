package org.example.agent.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentConfigLoaderTest {

    @Test
    void parseYaml_loadsNamedClientsAndPermissions() {
        AgentConfig config = AgentConfigLoader.parseYaml("""
                agent:
                  model:
                    active: deepseek
                    clients:
                      stub:
                        type: stub
                      deepseek:
                        type: deepseek
                        api-key: sk-test
                        base-url: https://api.deepseek.com
                        model: deepseek-v4-flash
                  permissions:
                    bash: ask
                    write: deny
                    network: allow
                """);

        assertEquals("deepseek", config.model().active());
        AgentConfig.ModelClientSettings deepseek = config.model().activeClient();
        assertEquals("deepseek", deepseek.type());
        assertEquals("sk-test", deepseek.apiKey());
        assertEquals("https://api.deepseek.com", deepseek.baseUrl());
        assertEquals("deepseek-v4-flash", deepseek.model());
        assertEquals("ask", config.permissions().bash());
        assertEquals("deny", config.permissions().write());
        assertEquals("allow", config.permissions().network());
    }

    @Test
    void activeClient_missing_throws() {
        AgentConfig config = new AgentConfig(
                new AgentConfig.ModelConfig("missing", Map.of(
                        "stub", AgentConfig.ModelClientSettings.stub())),
                AgentConfig.PermissionsConfig.defaults());
        assertThrows(IllegalStateException.class, config.model()::activeClient);
    }

    @Test
    void expandString_supportsDefault() {
        assertEquals("fallback", AgentConfigLoader.expandString("${UNLIKELY_ENV_VAR_XYZ:fallback}"));
        assertNull(AgentConfigLoader.expandString("${UNLIKELY_ENV_VAR_XYZ:}"));
    }
}
