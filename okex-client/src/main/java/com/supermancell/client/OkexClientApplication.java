package com.supermancell.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.supermancell.client", "com.supermancell.common"})
public class OkexClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(OkexClientApplication.class, args);
    }
}