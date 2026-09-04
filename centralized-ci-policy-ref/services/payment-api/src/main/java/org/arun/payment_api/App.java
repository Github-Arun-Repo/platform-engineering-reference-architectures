package org.arun.payment_api;

public class App {
    public String health() {
        return "payment-api:ok";
    }

    public static void main(String[] args) {
        System.out.println(new App().health());
    }
}
