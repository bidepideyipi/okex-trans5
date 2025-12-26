package com.supermancell.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.supermancell.server", "com.supermancell.common"})
public class OkexServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OkexServerApplication.class, args);
    }
}