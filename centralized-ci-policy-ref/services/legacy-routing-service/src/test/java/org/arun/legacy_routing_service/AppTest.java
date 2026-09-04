package org.arun.legacy_routing_service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppTest {
    @Test
    void healthEndpointContract() {
        assertEquals("legacy-routing-service:ok", new App().health());
    }

    @Test
    void routingContract() {
        App app = new App();
        assertEquals("A->B", app.route("A", "B"));
        assertEquals("invalid-route", app.route(null, "B"));
    }
}
