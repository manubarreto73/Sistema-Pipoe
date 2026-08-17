package com.pipoe.pipoeapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Scheduling: la poda nocturna del historial de documentos (PodaHistorialService).
@SpringBootApplication
@EnableScheduling
public class PipoeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipoeApiApplication.class, args);
    }
}
