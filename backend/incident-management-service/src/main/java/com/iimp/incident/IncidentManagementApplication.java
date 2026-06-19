package com.iimp.incident;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.iimp.incident", "com.iimp.common"})
@EnableCaching
@EnableKafka
@EnableScheduling
public class IncidentManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(IncidentManagementApplication.class, args);
    }
}
