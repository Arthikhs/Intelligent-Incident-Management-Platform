package com.iimp.health;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ServiceHealthAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceHealthAnalyzerApplication.class, args);
    }
}
