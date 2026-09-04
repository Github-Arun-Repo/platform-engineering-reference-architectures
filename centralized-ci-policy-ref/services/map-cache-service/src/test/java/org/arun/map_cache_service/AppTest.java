package org.arun.map_cache_service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @Test
    void healthEndpointContract() {
        assertEquals("map-cache-service:ok", new App().health());
    }

    @Test
    void cacheContract() {
        App app = new App();
        app.put("region", "us-east");
        assertEquals("us-east", app.get("region"));
        assertEquals("miss", app.get("unknown"));
    }
}
