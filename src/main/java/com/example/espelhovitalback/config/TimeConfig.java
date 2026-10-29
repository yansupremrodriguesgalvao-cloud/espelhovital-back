package com.example.espelhovitalback.config;

import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

@Configuration
@EnableScheduling
public class TimeConfig {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
