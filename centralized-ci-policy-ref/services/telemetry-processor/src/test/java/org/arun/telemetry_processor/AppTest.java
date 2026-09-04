package org.arun.telemetry_processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @Test
    void healthEndpointContract() {
        assertEquals("telemetry-processor:ok", new App().health());
    }

    @Test
    void telemetryProcessingContract() {
        App app = new App();
        assertEquals("telemetry:processed", app.process("event"));
        assertEquals("telemetry:empty", app.process(""));
    }
}
