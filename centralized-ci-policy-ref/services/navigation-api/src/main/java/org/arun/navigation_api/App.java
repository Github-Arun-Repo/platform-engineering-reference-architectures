package org.arun.navigation_api;

public class App {
    public String health() {
        return "navigation-api:ok";
    }

    public static void main(String[] args) {
        System.out.println(new App().health());
    }
}
