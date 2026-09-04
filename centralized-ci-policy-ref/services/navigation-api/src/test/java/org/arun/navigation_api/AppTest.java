package org.arun.navigation_api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @Test
    void healthEndpointContract() {
        assertEquals("navigation-api:ok", new App().health());
    }
}
