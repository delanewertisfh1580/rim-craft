package com.rimworldcraft.infrastructure.common;

import com.rimworldcraft.infrastructure.common.util.ConfigValidator;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** Tests common JSON configuration validation. */
class ConfigValidatorTest {
    @Test void validEnvelope_shouldPass() { assertThat(new ConfigValidator().validateJson("{\"schemaVersion\":1,\"traits\":[]}", "")).isTrue(); }
    @Test void missingSchemaVersion_shouldFail() { assertThat(new ConfigValidator().validateJson("{\"traits\":[]}", "")).isFalse(); }
    @Test void malformedJson_shouldFail() { assertThat(new ConfigValidator().validateJson("not-json", "")).isFalse(); }
}
