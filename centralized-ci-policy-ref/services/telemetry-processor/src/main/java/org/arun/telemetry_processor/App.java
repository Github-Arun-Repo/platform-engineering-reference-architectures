package org.arun.telemetry_processor;

public class App {
    public String process(String payload) {
        return payload == null || payload.isBlank() ? "telemetry:empty" : "telemetry:processed";
    }

    public String health() {
        return "telemetry-processor:ok";
    }

    public static void main(String[] args) {
        System.out.println(new App().health());
    }
}
