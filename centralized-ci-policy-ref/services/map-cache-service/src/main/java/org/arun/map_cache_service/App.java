package org.arun.map_cache_service;

import java.util.HashMap;
import java.util.Map;

public class App {
    private final Map<String, String> cache = new HashMap<>();

    public String put(String key, String value) {
        cache.put(key, value);
        return value;
    }

    public String get(String key) {
        return cache.getOrDefault(key, "miss");
    }

    public String health() {
        return "map-cache-service:ok";
    }

    public static void main(String[] args) {
        System.out.println(new App().health());
    }
}
