package org.arun.legacy_routing_service;

public class App {
    public String route(String source, String destination) {
        if (source == null || destination == null) {
            return "invalid-route";
        }
        return source + "->" + destination;
    }

    public String health() {
        return "legacy-routing-service:ok";
    }

    public static void main(String[] args) {
        System.out.println(new App().health());
    }
}
